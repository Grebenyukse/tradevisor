package ru.grnk.tradevisor.integration.finam.tradeclient.api;

import com.google.type.Decimal;
import grpc.tradeapi.v1.Side;
import grpc.tradeapi.v1.accounts.AccountsServiceGrpc;
import grpc.tradeapi.v1.accounts.GetAccountRequest;
import grpc.tradeapi.v1.accounts.GetAccountResponse;
import grpc.tradeapi.v1.accounts.Position;
import grpc.tradeapi.v1.assets.AssetsServiceGrpc;
import grpc.tradeapi.v1.auth.AuthRequest;
import grpc.tradeapi.v1.auth.AuthServiceGrpc;
import grpc.tradeapi.v1.orders.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.properties.TrvFinamProperties;
import ru.grnk.tradevisor.common.util.RoundPriceUtils;
import ru.grnk.tradevisor.integration.finam.BearerToken;
import ru.grnk.tradevisor.integration.finam.tradeclient.OpenPositionClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static ru.grnk.tradevisor.common.util.RoundPriceUtils.moneyToBigDecimal;
import static ru.grnk.tradevisor.common.util.RoundPriceUtils.roundPrice;


@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.integration.finam.open-position-client", havingValue = "grpc")
public class FinamGrpcTradeClient implements OpenPositionClient {

    public static final BigDecimal DEVIATION = BigDecimal.valueOf(0.95f);
    public static final String CLIENT_ORDER_TYPE_PART_2_ORDER_TYPE_SL = "SL";
    public static final String CLIENT_ORDER_TYPE_PART_2_ORDER_TYPE_OP = "OP";
    public static final String CLIENT_ORDER_TYPE_PART_2_ORDER_TYPE_TP = "TP";
    public static final BigDecimal AVERAGE_COMMISSION = BigDecimal.valueOf(0.002f);
    public static final BigDecimal GO_LEVEL = BigDecimal.valueOf(0.15f);
    public static final String CLIENT_ORDER_ID_SEPARATOR = "zz";
    private static final BigDecimal RISK_LEVEL = BigDecimal.valueOf(0.02);
    private final TradevisorProperties tradevisorProperties;

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
        var minStep = BigDecimal.valueOf(asset.getMinStep())
                .divide(BigDecimal.TEN.pow(asset.getDecimals()),
                        asset.getDecimals(),
                        RoundingMode.UNNECESSARY);
        BigDecimal normalizedPriceOpen = roundPrice(priceOpen, minStep, direction);
        BigDecimal normalizedStopLoss = roundPrice(stopLoss, minStep, direction);
        BigDecimal normalizedTakeProfit = roundPrice(takeProfit, minStep, direction);
        var tradingLot = getTradingLot(symbol, normalizedPriceOpen, normalizedStopLoss, normalizedTakeProfit, direction);
        if (tradingLot.intValue() == 0) {
            log.warn("not enough money to open position. signalId: {} , symbol:{}", signalId, symbol);
            return false;
        }
        placeOrders(symbol, normalizedPriceOpen, normalizedStopLoss, normalizedTakeProfit, direction, tradingLot.intValue(), signalId);
        return true;
    }

    /**
     * trading_lot = round.down(counted_lot, min_lot)
     * min_lot = минимальный лот, который можно выставить по инструменту
     * counted_lot = Min(risk_lot, available_lot)
     * risk_lot = (balance * risk_leve ) / (((price_open - stop_loss) / tick_size) * tick_price)
     * tick_steps = (price_open - stop_loss) / minimal_price_step
     * tick_price - стоимость шага цены
     * - получить через rts_api если есть, если нет = 1 руб
     * available_lot = available_money / (go_price * КПУР + ((price_open - sl) / tick_size) * tick_price + avg_commission_rate*go_price*lot_factor)
     * - go_price = direction= 1 ? tp : weighted_stop_loss (максимальное значение GO для позиции)
     * - lot_factor = количество элементов актива в одном лоте
     * - КПУР = 0.15 - ставка риска у брокера
     * - avg_commission_rate = 0,05 (5% на объем сделки)
     * - available_money
     * available_money = balance + variance_margin - sum(open_risk + locked_money + commission)
     * - balance = accountRs.getCashList().stream().filter(x -> x.getCurrencyCode().equals("RUB")).findFirst().map(RoundPriceUtils::moneyToBigDecimal).orElseThrow();
     * - variance_margin = вариационная маржа портфеля с момента открытия позиции
     * - sum - сумма по всем теоретическим позициям
     * - open_risk = ((weighted_price - weighted_sl) / tick_size) * tick_price * quantity
     * - weighted_price - средневзвешенная цена с учетом выставленных позиций и активных ордеров
     * - weighted_sl - средневзвешенная цена STOP_LOSS
     * - quantity - размер сделки в лотах
     * - commission = avg_commission_rate*go_price*lot_factor
     * - locked_money = go_price * quantity * КПУР
     * - quantity - размер позиции в лотах
     * - go_price =  direction = 1 ? tp : weighted_stop_loss
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
        BigDecimal tickPrice = BigDecimal.ONE; // TODO: add call rtsService
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
        BigDecimal slComponent = riskPerLot;
        BigDecimal commissionComponent = go.multiply(AVERAGE_COMMISSION);

        BigDecimal totalCostPerLot = lockedMarginComponent.add(slComponent).add(commissionComponent);

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
                .getOrdersList();
        var moneyLocked = accountRs.getPositionsList()
                .stream()
                .collect(Collectors.groupingBy(Position::getSymbol))
                .values()
                .stream()
                .filter(x -> !x.isEmpty())
                .map(positions -> getRiskForPosition(positions, orders))
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
        var slOrdersForPosition = orderStates.stream()
                .filter(o -> o.getOrder().getSymbol().equals(positionSymbol))
                .filter(o -> Objects.equals(getOrderTypeByClientOrderId(o.getOrder().getClientOrderId()), CLIENT_ORDER_TYPE_PART_2_ORDER_TYPE_SL))
                .toList();
        if (slOrdersForPosition.stream()
                .anyMatch(x -> x.getOrder().getSide() == positionSide)) {
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
        var weightedSlOrders = slOrdersForPosition.stream()
                .map(OrderState::getOrder)
                .map(x -> new PositionAvgPrice(bigDecimalFromDecimal(x.getLimitPrice()), bigDecimalFromDecimal(x.getQuantity())))
                .reduce(
                        new PositionAvgPrice(BigDecimal.ZERO, BigDecimal.ZERO),
                        (acc, p1) -> new PositionAvgPrice(weightedAvg(acc.price(), acc.weight(), p1.price(), p1.weight()), acc.weight().add(p1.weight()))
                );
        if (weightedPosition.weight().abs().compareTo(weightedSlOrders.weight().abs()) != 0) {
            log.error("position size not equal to stoploss size. position: {}, sl-orders: {}",
                    weightedPosition.weight(), weightedSlOrders.weight());
            throw new IllegalStateException();
        }
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
        return val1.multiply(val1Q).add(val2.multiply(val2Q)).divide(val1Q.add(val2Q), RoundingMode.HALF_EVEN);
    }

    private static BigDecimal bigDecimalFromDecimal(Decimal decimal) {
        return new BigDecimal(decimal.getValue());
    }

    private static String getOrderTypeByClientOrderId(String clientOrderId) {
        var parts = clientOrderId.split(CLIENT_ORDER_ID_SEPARATOR);
        if (parts.length != 3) throw new IllegalStateException("неверный client orderid: " + clientOrderId);
        return parts[1];
    }

    private OrderState placeOpenPositionOrder(String symbol, BigDecimal priceOpen, int direction, int lot, int signalId) {
        return ordersServiceBlockingStub.withCallCredentials(getBearer())
                .placeOrder(Order.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .setSymbol(symbol)
                        .setClientOrderId(getClientOrderId(signalId, CLIENT_ORDER_TYPE_PART_2_ORDER_TYPE_OP))
                        .setLimitPrice(Decimal.newBuilder().setValue(priceOpen.toString()).build())
                        .setType(OrderType.ORDER_TYPE_LIMIT)
                        .setTimeInForce(TimeInForce.TIME_IN_FORCE_DAY)
                        .setQuantity(Decimal.newBuilder().setValue(String.valueOf(lot)).build())
                        .setSide(direction > 0 ? Side.SIDE_BUY : Side.SIDE_SELL)
                        .build());
    }

    private OrderState placeStopLossOrder(String symbol, BigDecimal stopLoss, int direction, int lot, int signalId) {
        return ordersServiceBlockingStub.withCallCredentials(getBearer())
                .placeOrder(Order.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .setSymbol(symbol)
                        .setClientOrderId(getClientOrderId(signalId, CLIENT_ORDER_TYPE_PART_2_ORDER_TYPE_SL))
                        .setLimitPrice(Decimal.newBuilder().setValue(stopLoss.toString()).build())
                        .setStopPrice(Decimal.newBuilder().setValue(stopLoss.toString()).build())
                        .setStopCondition(direction > 0 ? StopCondition.STOP_CONDITION_LAST_DOWN : StopCondition.STOP_CONDITION_LAST_UP)
                        .setType(OrderType.ORDER_TYPE_STOP_LIMIT)
                        .setTimeInForce(TimeInForce.TIME_IN_FORCE_GOOD_TILL_CANCEL)
                        .setQuantity(Decimal.newBuilder().setValue(String.valueOf(lot)).build())
                        .setSide(direction > 0 ? Side.SIDE_SELL : Side.SIDE_BUY)
                        .build());
    }

    private OrderState placeTakeProfitOrder(String symbol, BigDecimal takeProfit, int direction, int lot, int signalId) {
        return ordersServiceBlockingStub.withCallCredentials(getBearer())
                .placeOrder(Order.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .setSymbol(symbol)
                        .setClientOrderId(getClientOrderId(signalId, CLIENT_ORDER_TYPE_PART_2_ORDER_TYPE_TP))
                        .setLimitPrice(Decimal.newBuilder().setValue(takeProfit.toString()).build())
                        .setStopPrice(Decimal.newBuilder().setValue(takeProfit.toString()).build())
                        .setStopCondition(direction > 0 ? StopCondition.STOP_CONDITION_LAST_UP : StopCondition.STOP_CONDITION_LAST_DOWN)
                        .setType(OrderType.ORDER_TYPE_STOP_LIMIT)
                        .setTimeInForce(TimeInForce.TIME_IN_FORCE_GOOD_TILL_CANCEL)
                        .setQuantity(Decimal.newBuilder().setValue(String.valueOf(lot)).build())
                        .setSide(direction > 0 ? Side.SIDE_SELL : Side.SIDE_BUY)
                        .build());
    }

    private void placeOrders(String symbol,
                             BigDecimal priceOpen,
                             BigDecimal stopLoss,
                             BigDecimal takeProfit,
                             int direction,
                             int lot,
                             int signalId) {
        var openPositionOrderRes = placeOpenPositionOrder(symbol, priceOpen, direction, lot, signalId);
        if (!openPositionOrderRes.isInitialized()) {
            throw new IllegalStateException("order placed incorrectly");
        }
        var stopLossOrderRes = placeStopLossOrder(symbol, stopLoss, direction, lot, signalId);
        if (!stopLossOrderRes.isInitialized()) {
            throw new IllegalStateException("order placed incorrectly");
        }
        var takeProfitRes = placeTakeProfitOrder(symbol, takeProfit, direction, lot, signalId);
        if (!takeProfitRes.isInitialized()) {
            throw new IllegalStateException("order placed incorrectly");
        }
        log.info("ордеры размещены. Open: {}, StopLoss: {}, TakeProfit: {}", openPositionOrderRes, stopLossOrderRes, takeProfitRes);
    }

    /**
     * clientOrderId = signalId;TP|OP|SL;timestamp
     */
    public String getClientOrderId(int signalId, String type) {
        StringBuilder sb = new StringBuilder(signalId + CLIENT_ORDER_ID_SEPARATOR + type + CLIENT_ORDER_ID_SEPARATOR + System.currentTimeMillis());
        return sb.length() > 20 ? sb.substring(0, 20) : sb.toString();
    }

}
