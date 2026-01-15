package ru.grnk.tradevisor.integration.finam.tradeclient.mt5py;

import com.google.type.Decimal;
import grpc.tradeapi.v1.Side;
import grpc.tradeapi.v1.accounts.AccountsServiceGrpc;
import grpc.tradeapi.v1.accounts.GetAccountRequest;
import grpc.tradeapi.v1.accounts.GetAccountResponse;
import grpc.tradeapi.v1.accounts.Position;
import grpc.tradeapi.v1.assets.AssetsServiceGrpc;
import grpc.tradeapi.v1.auth.AuthRequest;
import grpc.tradeapi.v1.auth.AuthServiceGrpc;
import grpc.tradeapi.v1.orders.OrderState;
import grpc.tradeapi.v1.orders.OrdersRequest;
import grpc.tradeapi.v1.orders.OrdersServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.properties.TrvFinamProperties;
import ru.grnk.tradevisor.common.util.RoundPriceUtils;
import ru.grnk.tradevisor.integration.finam.BearerToken;
import ru.grnk.tradevisor.integration.finam.tradeclient.OpenPositionClient;
import ru.grnk.tradevisor.integration.rts.RtsService;
import ru.grnk.tradevisor.integration.rts.dto.ContractParams;
import ru.ttech.piapi.core.helpers.NumberMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static ru.grnk.tradevisor.common.util.RoundPriceUtils.moneyToBigDecimal;
import static ru.grnk.tradevisor.common.util.RoundPriceUtils.roundPrice;
import static ru.grnk.tradevisor.integration.finam.FinamTradeClient.NOT_ACTIVE_ORDER_STATUSES;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.integration.finam.open-position-client", havingValue = "mt5py")
public class FinamMt5PythonClient implements OpenPositionClient {

    private final RestTemplate restTemplate;
    private final TradevisorProperties tradevisorProperties;
    public static final BigDecimal DEVIATION = BigDecimal.valueOf(0.95f);
    public static final BigDecimal AVERAGE_COMMISSION = BigDecimal.valueOf(0.002f);
    public static final BigDecimal GO_LEVEL = BigDecimal.valueOf(0.15f);
    private static final BigDecimal RISK_LEVEL = BigDecimal.valueOf(0.02);
    private final RtsService rtsService;

    private final AccountsServiceGrpc.AccountsServiceBlockingStub accountsServiceBlockingStub;
    private final OrdersServiceGrpc.OrdersServiceBlockingStub ordersServiceBlockingStub;
    private final AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub;
    private final AssetsServiceGrpc.AssetsServiceBlockingStub assetsServiceBlockingStub;

    @Override
    public boolean openPosition(String symbol, float priceOpen, float stopLoss, float takeProfit, int direction, int signalId) {
        var assetParams = getAssetParams(symbol);
        if (!assetParams.getTradeable()) {
            log.warn("symbol is not tradeable: {}", symbol);
            return false;
        }
        if (assetParams.getLongable().getValueValue() == 0) { //log запрещен. мы таким не торгуем
            log.warn("long operations restricted for symbol: {}", symbol);
            return false;
        }
        if (direction == -1 && assetParams.getShortable().getValue().getNumber() == 0) {
            log.warn("short operations restricted and signal for sell for symbol: {}", symbol);
            return false;
        }
        var asset = getAsset(symbol);
        var minPriceStep = BigDecimal.valueOf(asset.getMinStep())
                .divide(BigDecimal.TEN.pow(asset.getDecimals()),
                        asset.getDecimals(),
                        RoundingMode.UNNECESSARY);
        BigDecimal normalizedPriceOpen = roundPrice(priceOpen, minPriceStep, direction);
        BigDecimal normalizedStopLoss = roundPrice(stopLoss, minPriceStep, direction);
        BigDecimal normalizedTakeProfit = roundPrice(takeProfit, minPriceStep, direction);
        var tradingLot = getTradingLot(symbol, normalizedPriceOpen, normalizedStopLoss, normalizedTakeProfit, direction);
        if (tradingLot.intValue() == 0) {
            log.warn("not enough money to open position. signalId: {} , symbol:{}", signalId, symbol);
            return false;
        }
        return openPositionMt5(symbol, normalizedPriceOpen, normalizedStopLoss, normalizedTakeProfit, direction, tradingLot.intValue(), signalId);
    }

    /**
     * trading_lot = round.down(counted_lot, min_lot)
     * min_lot = минимальный лот, который можно выставить по инструменту
     * counted_lot = Min(risk_lot, available_lot)
     * risk_lot = (balance * risk_leve ) / (((price_open - stop_loss) / tick_size) * tick_price)
     * tick_steps = (price_open - stop_loss) / minimal_price_step
     * tick_price - стоимость шага цены
     * - получить через rts_api если есть, если нет = 1 руб
     * available_lot = available_money / (go + ((price_open - sl) / tick_size) * tick_price + avg_commission_rate*go_price*lot_factor)
     * - go = direction= 1 ? longCollateral : shortCollateral. данные от брокера
     * - lot_factor = количество элементов актива в одном лоте
     * - avg_commission_rate = 0,05 (5% на объем сделки)
     * - available_money
     * available_money = balance + variance_margin - sum(open_risk + locked_money + commission)
     * - balance = accountRs.getCashList().stream().filter(x -> x.getCurrencyCode().equals("RUB")).findFirst().map(RoundPriceUtils::moneyToBigDecimal).orElseThrow();
     * - variance_margin = вариационная маржа портфеля с момента открытия позиции
     * - sum - сумма по всем теоретическим позициям
     * - open_risk = ((last_clearing_price - weighted_sl) / tick_size) * tick_price * quantity
     * - last_clearing_price - цена последнего клиринга
     * - weighted_sl - средневзвешенная цена STOP_LOSS
     * - quantity - размер сделки в лотах
     * - commission = (open_position_fee + close_position_fee + clearing_fee*position_hold + overnight_fee*average_position_hold)*quantity
     * - locked_money = quantity * go
     * - quantity - размер позиции в лотах
     * params:
     * BigDecimal priceOpen - нормализованная цена открытия (округлена до ближайшего тика с учетом tick_size)
     * BigDecimal stopLoss - нормализованная цена stop loss (округлена до ближайшего тика с учетом tick_size)
     * BigDecimal takeProfit - нормализованная цена take profit (округлена до ближайшего тика с учетом tick_size)
     * int direction - направление 1 - long, -1 short
     *
     * @return lot amount to trade
     */
    private BigDecimal getTradingLot(String symbol, BigDecimal priceOpen, BigDecimal stopLoss, BigDecimal takeProfit, int direction) {
        var accountRs = getAccount();
        var balance = accountRs.getCashList().stream()
                .filter(x -> x.getCurrencyCode().equals("RUB"))
                .findFirst()
                .map(RoundPriceUtils::moneyToBigDecimal)
                .orElse(BigDecimal.ZERO);
        var availableMoney = getAvailableMoney(balance, accountRs);
        var assetParams = getAssetParams(symbol);
        var asset = getAsset(symbol);
        var priceMinStep = BigDecimal.valueOf(asset.getMinStep())
                .divide(BigDecimal.TEN.pow(asset.getDecimals()),
                        asset.getDecimals(),
                        RoundingMode.UNNECESSARY);
        var lotSize = bigDecimalFromDecimal(asset.getLotSize());
        BigDecimal go = direction == 1 ? moneyToBigDecimal(assetParams.getLongCollateral()) : moneyToBigDecimal(assetParams.getShortCollateral());
        var ticker = symbol.split("@")[0];
        BigDecimal tickPrice = ofNullable(rtsService.getContractParams(ticker))
                .map(ContractParams::getFullTickValue)
                .orElse(BigDecimal.ONE);
        if (!isValidPriceConfiguration(priceOpen, stopLoss, takeProfit, direction)) {
            throw new IllegalStateException("invalid sl or tp ");
        }
        BigDecimal riskPerLot = calculateRiskPerLot(priceOpen, stopLoss, priceMinStep, tickPrice);
        if (riskPerLot.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        // risk_lot = (balance * risk_level) / risk_per_lot
        BigDecimal riskLot = balance.multiply(RISK_LEVEL)
                .divide(riskPerLot, RoundingMode.DOWN);
        BigDecimal availableLot = calculateAvailableLot(go, riskPerLot, availableMoney);
        BigDecimal countedLot = riskLot.min(availableLot);

        if (lotSize.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Округляем до целых лотов
        BigDecimal tradingLot = countedLot.setScale(0, RoundingMode.DOWN);

        return tradingLot.max(BigDecimal.ZERO);
    }

    public BearerToken getBearer() {
        TrvFinamProperties finamProperties = tradevisorProperties.integration().finam();
        var authRs = authServiceBlockingStub.auth(AuthRequest.newBuilder()
                .setSecret(finamProperties.secret())
                .build());
        return new BearerToken(authRs.getToken());
    }

    private grpc.tradeapi.v1.assets.GetAssetResponse getAsset(String symbol) {
        return assetsServiceBlockingStub.withCallCredentials(getBearer())
                .getAsset(grpc.tradeapi.v1.assets.GetAssetRequest.newBuilder()
                        .setSymbol(symbol)
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .build());
    }

    private grpc.tradeapi.v1.assets.GetAssetParamsResponse getAssetParams(String symbol) {
        return assetsServiceBlockingStub.withCallCredentials(getBearer())
                .getAssetParams(grpc.tradeapi.v1.assets.GetAssetParamsRequest.newBuilder()
                        .setSymbol(symbol)
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .build());
    }

    private GetAccountResponse getAccount() {
        return accountsServiceBlockingStub.withCallCredentials(getBearer())
                .getAccount(GetAccountRequest.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .build());
    }

    private boolean isValidPriceConfiguration(BigDecimal priceOpen, BigDecimal stopLoss, BigDecimal takeProfit, int direction) {
        if (direction > 0) {
            // LONG: SL должен быть ниже цены открытия, TP выше
            if (stopLoss.compareTo(priceOpen) >= 0) {
                log.warn("Invalid stop loss for LONG position. SL: {}, Open: {}", stopLoss, priceOpen);
                return false;
            }
            if (takeProfit.compareTo(priceOpen) <= 0) {
                log.warn("Invalid take profit for LONG position. TP: {}, Open: {}", takeProfit, priceOpen);
                return false;
            }
        } else {
            // SHORT: SL должен быть выше цены открытия, TP ниже
            if (stopLoss.compareTo(priceOpen) <= 0) {
                log.warn("Invalid stop loss for SHORT position. SL: {}, Open: {}", stopLoss, priceOpen);
                return false;
            }
            if (takeProfit.compareTo(priceOpen) >= 0) {
                log.warn("Invalid take profit for SHORT position. TP: {}, Open: {}", takeProfit, priceOpen);
                return false;
            }
        }
        return true;
    }

    private BigDecimal calculateRiskPerLot(BigDecimal priceOpen, BigDecimal stopLoss, BigDecimal minStep, BigDecimal tickPrice) {
        return priceOpen.subtract(stopLoss)
                .abs()
                .divide(minStep, RoundingMode.HALF_UP)
                .multiply(tickPrice);
    }

    private BigDecimal calculateAvailableLot(BigDecimal go, BigDecimal riskPerLot, BigDecimal availableMoney) {
        BigDecimal lockedMarginComponent = go.multiply(GO_LEVEL);
        BigDecimal commissionComponent = go.multiply(AVERAGE_COMMISSION);
        BigDecimal totalCostPerLot = lockedMarginComponent.add(riskPerLot).add(commissionComponent);
        if (totalCostPerLot.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return availableMoney.divide(totalCostPerLot, RoundingMode.DOWN);
    }

    private BigDecimal getAvailableMoney(BigDecimal balance, GetAccountResponse accountRs) {
        var bearer = getBearer();
        List<OrderState> orders = ordersServiceBlockingStub.withCallCredentials(bearer)
                .getOrders(OrdersRequest.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .build())
                .getOrdersList()
                .stream()
                .filter(x -> !NOT_ACTIVE_ORDER_STATUSES.contains(x.getStatus()))
                .toList();
        var moneyLocked = accountRs.getPositionsList()
                .stream()
                .filter(x -> bigDecimalFromDecimal(x.getQuantity()).abs().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.groupingBy(Position::getSymbol))
                .values()
                .stream()
                .filter(x -> !x.isEmpty())
                .map(position -> getRiskForPosition(position, orders))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return balance.subtract(moneyLocked).multiply(DEVIATION).max(BigDecimal.ZERO);
    }

    private record PositionAvgPrice(BigDecimal price, BigDecimal weight) {
    }

    private BigDecimal getRiskForPosition(List<Position> positions, List<OrderState> orderStates) {
        Position anyPosition = positions.stream().findFirst().orElseThrow();
        var positionQuantity = bigDecimalFromDecimal(anyPosition.getQuantity());
        var positionSide = positionQuantity.signum() > 0 ? Side.SIDE_BUY : Side.SIDE_SELL;
        var positionSymbol = anyPosition.getSymbol();
        var assetParams = getAssetParams(positionSymbol);
        int direction = positionQuantity.signum();
        BigDecimal go = direction == 1 ?
                moneyToBigDecimal(assetParams.getLongCollateral()) :
                moneyToBigDecimal(assetParams.getShortCollateral());
        // если купили, то stopLoss - самая малая цена. сортируем по возрастанию и берем первую
        // если продали, то StopLoss - самая большая цена. сортируем по убыванию и берем первую
        var slOrdersForPosition = orderStates.stream()
                .filter(o -> o.getOrder().getSymbol().equals(positionSymbol))
                .min((x1, x2) -> {
                    // если купили, то stopLoss - самая малая цена. сортируем по возрастанию и берем первую
                    // если продали, то StopLoss - самая большая цена. сортируем по убыванию и берем первую
                    var directionMultiplier = x1.getOrder().getSide() == Side.SIDE_BUY ? 1 : -1;
                    return directionMultiplier * bigDecimalFromDecimal(x1.getOrder().getLimitPrice())
                            .compareTo(bigDecimalFromDecimal(x2.getOrder().getLimitPrice()));
                })
                .orElseThrow();
        if (slOrdersForPosition.getOrder().getSide() == positionSide) {
            throw new IllegalStateException("found stop loss and positions in same direction. Иди проверь все позиции руками. Что происходит?");
        }
        if (positions.stream()
                .map(x -> bigDecimalFromDecimal(x.getQuantity()).signum())
                .distinct()
                .count() > 1) {
            throw new IllegalStateException("found positions for symbol in different directions. GO CHECK THIS.");
        }
        var weightedPosition = positions.stream()
                .map(x -> new PositionAvgPrice(bigDecimalFromDecimal(x.getAveragePrice()), bigDecimalFromDecimal(x.getQuantity())))
                .reduce(
                        new PositionAvgPrice(BigDecimal.ZERO, BigDecimal.ZERO),
                        (acc, p1) -> new PositionAvgPrice(weightedAvg(acc.price(), acc.weight(), p1.price(), p1.weight()), acc.weight().add(p1.weight()))
                );
        // даже если ордеров на стоп несколько используем самую "плохую цену" для оценки сверху.
        var weightedSlOrders =  new PositionAvgPrice(bigDecimalFromDecimal(slOrdersForPosition.getOrder().getLimitPrice()), weightedPosition.weight());
        BigDecimal openRisk = weightedPosition.price()
                .subtract(weightedSlOrders.price())
                .multiply(weightedPosition.weight())
                .abs();
        var commission = positionQuantity.multiply(
                        weightedPosition.price().max(weightedSlOrders.price())
                )
                .multiply(AVERAGE_COMMISSION);
        var lockedMoney = weightedPosition.weight().multiply(go);
        return openRisk.abs().add(commission).add(lockedMoney);
    }

    private static BigDecimal weightedAvg(BigDecimal val1, BigDecimal val1Q, BigDecimal val2, BigDecimal val2Q) {
        BigDecimal divisor = val1Q.add(val2Q);
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return val1.multiply(val1Q).add(val2.multiply(val2Q)).divide(divisor, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal bigDecimalFromDecimal(Decimal decimal) {
        return new BigDecimal(decimal.getValue());
    }

    public boolean openPositionMt5(String tickerCode, BigDecimal priceOpen, BigDecimal stopLoss, BigDecimal takeProfit, int direction, int quantity, int signalId) {
        var baseUrl = tradevisorProperties.integration().finam().mt5PythonClientUrl();
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/trade/open-position")
                .toUriString();
        var rq = OpenPositionRq.builder()
                .symbol(tickerCode.split("@")[0])
                .price_open(bigDecimalToPrice(priceOpen))
                .stop_loss(bigDecimalToPrice(stopLoss))
                .take_profit(bigDecimalToPrice(takeProfit))
                .direction(direction)
                .quantity(quantity)
                .build();
        try {
            var res = restTemplate.postForObject(url, rq, OpenPositionRs.class);
            if (res == null) {
                log.error("Received null response from MT5 Python client for tickerCode: {}. signalId: {}", tickerCode, signalId);
                return false;
            }
            log.info(res.toString());
            // Проверяем, что lot открытой позиции не равен 0
            if (res.lot() == 0) {
                log.error("Position opened with zero lot for tickerCode: {}", tickerCode);
                return false;
            }
            return true;
        } catch (HttpClientErrorException e) {
            // Обработка 4XX ошибок
            log.error("Client error ({} {}) when opening position for tickerCode {}: {}",
                    e.getStatusCode().value(), e.getStatusText(), tickerCode, e.getResponseBodyAsString());
            return false;
        } catch (HttpServerErrorException e) {
            // Обработка 5XX ошибок
            log.error("Server error ({} {}) when opening position for tickerCode {}: {}",
                    e.getStatusCode().value(), e.getStatusText(), tickerCode, e.getResponseBodyAsString());
            return false;
        } catch (ResourceAccessException e) {
            // Обработка сетевых ошибок
            log.error("Network error when opening position for tickerCode {}: {}", tickerCode, e.getMessage());
            return false;
        } catch (Exception e) {
            // Обработка других непредвиденных ошибок
            log.error("Unexpected error when opening position for tickerCode {}: {}", tickerCode, e.getMessage(), e);
            return false;
        }
    }

    private Price bigDecimalToPrice(BigDecimal price) {
        var quotation = NumberMapper.bigDecimalToQuotation(price);
        return new Price(quotation.getUnits(), quotation.getNano());
    }
}
