package ru.grnk.tradevisor.integration.finam.tradeclient.api;

import com.google.type.Decimal;
import grpc.tradeapi.v1.Side;
import grpc.tradeapi.v1.accounts.AccountsServiceGrpc;
import grpc.tradeapi.v1.accounts.GetAccountRequest;
import grpc.tradeapi.v1.accounts.GetAccountResponse;
import grpc.tradeapi.v1.accounts.Position;
import grpc.tradeapi.v1.assets.AssetsServiceGrpc;
import grpc.tradeapi.v1.assets.GetAssetParamsRequest;
import grpc.tradeapi.v1.assets.GetAssetRequest;
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
    private final TradevisorProperties tradevisorProperties;

    private final AccountsServiceGrpc.AccountsServiceBlockingStub accountsServiceBlockingStub;
    private final OrdersServiceGrpc.OrdersServiceBlockingStub ordersServiceBlockingStub;
    private final AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub;
    private final AssetsServiceGrpc.AssetsServiceBlockingStub assetsServiceBlockingStub;

    public BearerToken getBearer() {
        TrvFinamProperties finamProperties = tradevisorProperties.integration().finam();
        var authRs = authServiceBlockingStub.auth(AuthRequest.newBuilder()
                .setSecret(finamProperties.secret())
                .build());
        return new BearerToken(authRs.getToken());
    }

    /**
     * 1. получить остаток свободных средствв
     * 2. округлить цены по тикеру
     * 3. получить минимальный шаг цены
     * 4. получить стоимость шага цены на 1 лот
     * 5. получить минимальный лот
     * 6. получить максимально возможный лот под текущие средства
     * 7. если лот < min lot -> выти
     * 8. получить рассчетный лот под размер риска в настройках
     * 9. выставить ордера
     *
     * @param symbol     - "gazp@RTSX"
     * @param priceOpen  - приблизительная цена (требует округления)
     * @param stopLoss   - приблизительная цена (требует округления)
     * @param takeProfit - приблизительная цена (требует округления)
     * @param direction  1 - buy, -1 - sell
     * @return
     */
    @Override
    public boolean openPosition(String symbol, float priceOpen, float stopLoss, float takeProfit, int direction, int signalId) {
        var bearer = getBearer();
        var accountId = tradevisorProperties.integration().finam().accountId();
        var assetParams = assetsServiceBlockingStub.withCallCredentials(bearer)
                .getAssetParams(GetAssetParamsRequest.newBuilder()
                        .setSymbol(symbol)
                        .setAccountId(accountId)
                        .build());
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
        var accountRs = accountsServiceBlockingStub.withCallCredentials(bearer)
                .getAccount(GetAccountRequest.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .build());
        var balance = accountRs.getCashList().stream()
                .filter(x -> x.getCurrencyCode().equals("RUB"))
                .findFirst()
                .map(RoundPriceUtils::moneyToBigDecimal)
                .orElseThrow();
        var asset = assetsServiceBlockingStub.withCallCredentials(bearer)
                .getAsset(GetAssetRequest.newBuilder()
                        .setSymbol(symbol)
                        .setAccountId(accountId)
                        .build());
        var minStep = BigDecimal.valueOf(asset.getMinStep())
                .divide(BigDecimal.TEN.pow(asset.getDecimals()),
                        asset.getDecimals(),
                        RoundingMode.UNNECESSARY);
        BigDecimal normalizedPriceOpen = roundPrice(priceOpen, minStep, direction);
        BigDecimal normalizedStopLoss = roundPrice(stopLoss, minStep, direction);
        BigDecimal normalizedTakeProfit = roundPrice(takeProfit, minStep, direction);
        BigDecimal availableMoney = getAvailableMoney(balance, accountRs);
        var go = direction == 1 ? assetParams.getLongCollateral() : assetParams.getShortCollateral();
        BigDecimal ticksCountRisk = normalizedPriceOpen.subtract(normalizedStopLoss).abs().divide(minStep, RoundingMode.UNNECESSARY);
        BigDecimal tickPrice = BigDecimal.ONE; // получить через RTS сервис
        BigDecimal riskMoney = availableMoney.min(balance.multiply(BigDecimal.valueOf((double)tradevisorProperties.trade().limits() / 100)));
        var tradingLot =
                riskMoney.divide(tickPrice.multiply(ticksCountRisk), RoundingMode.DOWN).min(
                        availableMoney.divide(moneyToBigDecimal(go), RoundingMode.DOWN)
                );
        if (tradingLot.intValue() == 0) {
            log.warn("not enough money to open position. signalId: {} , symbol:{}", signalId, symbol);
            return false;
        }
        placeOrders(symbol, normalizedPriceOpen, normalizedStopLoss, normalizedTakeProfit, direction, tradingLot.intValue(), signalId);
        return true;
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
        return balance.subtract(moneyLocked).multiply(DEVIATION);
    }

    private record PositionAvgPrice(BigDecimal price, BigDecimal weight) {
    }

    private BigDecimal getRiskForPosition(List<Position> positions, List<OrderState> orderStates) {
        Position anyPosition = positions.stream().findFirst().orElseThrow();
        var positionSide = new BigDecimal(anyPosition.getQuantity().getValue()).signum() > 0 ? Side.SIDE_BUY : Side.SIDE_SELL;
        var slOrdersForPosition = orderStates.stream()
                .filter(o -> o.getOrder().getSymbol().equals(anyPosition.getSymbol()))
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
        if (weightedPosition.weight().compareTo(weightedSlOrders.weight()) != 0) {
            log.error("position size not equal to stoploss size. position: {}, sl-orders: {}",
                    weightedPosition.weight(), weightedSlOrders.weight());
            throw new IllegalStateException();
        }
        var openRisk = weightedPosition.price().subtract(weightedSlOrders.price()).multiply(weightedPosition.weight());
        var commission = weightedPosition.weight().multiply(AVERAGE_COMMISSION);
        var lockedMoney = weightedPosition.weight().multiply(GO_LEVEL);
        return openRisk.add(commission).add(lockedMoney);
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

    private void placeOrders(String symbol,
                             BigDecimal priceOpen,
                             BigDecimal stopLoss,
                             BigDecimal takeProfit,
                             int direction,
                             int lot,
                             int signalId) {
        var openPositionOrderRes = ordersServiceBlockingStub.withCallCredentials(getBearer())
                .placeOrder(Order.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .setSymbol(symbol)
                        .setClientOrderId(getClientOrderId(signalId, CLIENT_ORDER_TYPE_PART_2_ORDER_TYPE_OP))
                        .setLimitPrice(Decimal.newBuilder().setValue(priceOpen.toString()).build())
                        .setType(OrderType.ORDER_TYPE_LIMIT)
                        .setTimeInForce(TimeInForce.TIME_IN_FORCE_GOOD_TILL_CANCEL)
                        .setQuantity(Decimal.newBuilder().setValue(String.valueOf(lot)).build())
                        .setSide(direction > 0 ? Side.SIDE_BUY : Side.SIDE_SELL)
                        .build());
        if (!openPositionOrderRes.isInitialized()) {
            throw new IllegalStateException("order placed incorrectly");
        }
        var stopLossOrderRes = ordersServiceBlockingStub.withCallCredentials(getBearer())
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
        if (!stopLossOrderRes.isInitialized()) {
            throw new IllegalStateException("order placed incorrectly");
        }
        var takeProfitRes = ordersServiceBlockingStub.withCallCredentials(getBearer())
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
