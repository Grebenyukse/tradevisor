package ru.grnk.tradevisor.integration.finam;

import com.google.type.Decimal;
import com.google.type.Money;
import grpc.tradeapi.v1.Side;
import grpc.tradeapi.v1.accounts.AccountsServiceGrpc;
import grpc.tradeapi.v1.accounts.GetAccountRequest;
import grpc.tradeapi.v1.auth.AuthRequest;
import grpc.tradeapi.v1.auth.AuthServiceGrpc;
import grpc.tradeapi.v1.orders.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.properties.TrvFinamProperties;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.entity.Signals;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.grnk.tradevisor.trade.TradeClient;
import ru.grnk.tradevisor.trade.dto.TrvOrder;
import ru.grnk.tradevisor.trade.dto.TrvPosition;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static grpc.tradeapi.v1.orders.TimeInForce.TIME_IN_FORCE_GOOD_TILL_CANCEL;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinamTradeClient implements TradeClient {

    public static final double NANOS_DIGITS = Math.pow(10, -9);
    private final TradevisorProperties tradevisorProperties;
    private final AccountsServiceGrpc.AccountsServiceBlockingStub accountsServiceBlockingStub;
    private final OrdersServiceGrpc.OrdersServiceBlockingStub ordersServiceBlockingStub;
    private final AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub;
    private final TickersRepository tickersRepository;

    public BearerToken getBearer() {
        TrvFinamProperties finamProperties = tradevisorProperties.integration().finam();
        var authRs = authServiceBlockingStub.auth(AuthRequest.newBuilder()
                .setSecret(finamProperties.secret())
                .build());
        return new BearerToken(authRs.getToken());
    }

    @Override
    public String provider() {
        return "finam";
    }

    @Override
    public Float getBalance() {
        var bearer = getBearer();
        var accountRs = accountsServiceBlockingStub
                .withCallCredentials(bearer)
                .getAccount(GetAccountRequest.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .build());
        return Float.parseFloat(accountRs.getCashList().stream()
                .filter(x -> x.getCurrencyCode().equals("RUB"))
                .findFirst()
                .orElseThrow()
                .toString()
        );
    }

    @Override
    public Float getFreeMargin() {
        var bearer = getBearer();
        var accountRs = accountsServiceBlockingStub
                .withCallCredentials(bearer)
                .getAccount(GetAccountRequest.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .build());
        return Float.parseFloat(accountRs.getEquity().getValue());
    }

    @Override
    public String findTickerForSpot(String tickerCode) {
        try {
            Tickers spotTicker = tickersRepository.getTickerByTickerCode(tickerCode);
            return spotTicker.getTicker();
        } catch (Exception e) {
            log.error("Error finding futures contract for spot ticker: {}", tickerCode, e);
            return null;
        }
    }

    @Override
    public Float getTickPriceForTicker(String tickerCode) {
        Tickers ticker = tickersRepository.getTickerByTickerCode(tickerCode);
        var tickPrice = ticker.getLot() * Math.pow(10, -1 * ticker.getPrecision());
        var currencyMultiplier = Objects.equals(ticker.getCurrency(), "RUB") ? 1 : 90;  // средний курс доллара на год
        return (float) tickPrice * currencyMultiplier;
    }


    @Override
    public Float getMinLotForTicker(String tickerCode) {
        return 1f;
    }

    @Override
    public List<TrvOrder> getOrdersByTicker(String tickerCode) {
        var bearer = getBearer();
        var orders = ordersServiceBlockingStub.withCallCredentials(bearer)
                .getOrders(OrdersRequest.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .build());
        if (orders.getOrdersList().isEmpty()) {
            return List.of();
        } else {
            throw new NotImplementedException();
        }
    }

    @Override
    public TrvPosition getAvgPositionByTicker(String tickerCode) {
        Tickers ticker = tickersRepository.getTickerByTickerCode(tickerCode);
        var bearer = getBearer();
        var accountResponse = accountsServiceBlockingStub
                .withCallCredentials(bearer)
                .getAccount(GetAccountRequest.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .build());
        var positions = accountResponse.getPositionsList();
        var positionForSymbol = positions.stream().filter(p -> p.getSymbol().equals(tickerCode))
                .findFirst()
                .orElse(null);
        if (positionForSymbol == null) {
            log.info("position for tickercode:{} not found", tickerCode);
            return null;
        }
        var openLots = (int) (Float.parseFloat(positionForSymbol.getQuantity().getValue()) / ticker.getLot());
        int direction = (int) Math.signum(openLots);

        OrdersResponse orders = ordersServiceBlockingStub
                .withCallCredentials(bearer)
                .getOrders(OrdersRequest.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .build());
        List<OrderState> ordersForTicker = orders.getOrdersList().stream()
                .filter(o -> o.getOrder().getSymbol().equals(tickerCode))
                .sorted(Comparator.comparing(
                        o -> Float.parseFloat(o.getOrder().getLimitPrice().getValue()),
                        Comparator.reverseOrder()
                ))
                .toList();
        if (ordersForTicker.size() > 2) {
            log.error("найдено более двух открытых ордеров для одной позиции. Должно быть только 2. " +
                    "рекомендуется перевыставить ордера. {}", ordersForTicker
                    .stream()
                    .map(x -> x.getOrder().getClientOrderId()).collect(Collectors.joining(", ")));
            throw new IllegalStateException();
        }
        OrderState tpOrder;
        OrderState slOrder;
        if (direction > 0) {
            tpOrder = ordersForTicker.get(0);
            slOrder = ordersForTicker.get(1);
        } else {
            tpOrder = ordersForTicker.get(1);
            slOrder = ordersForTicker.get(0);
        }

        if (tpOrder.getOrder().getQuantity() != positionForSymbol.getQuantity() ||
                slOrder.getOrder().getQuantity() != positionForSymbol.getQuantity()) {
            log.error("не совпадает количество лотов в позиции и в ордерах profit. SL: {}. TP: {}. Position: {}",
                    slOrder.getOrder().getQuantity(),
                    tpOrder.getOrder().getQuantity(),
                    positionForSymbol.getQuantity());
            throw new IllegalStateException();
        }
        return TrvPosition.builder()
                .tickerCode(tickerCode)
                .price(Float.parseFloat(positionForSymbol.getAveragePrice().getValue()))
                .lot(Math.abs(openLots))
                .direction(direction)
                .tp(Float.parseFloat(tpOrder.getOrder().getLimitPrice().getValue()))
                .sl(Float.parseFloat(slOrder.getOrder().getLimitPrice().getValue()))
                .build();
    }

    @Override
    public void setOrder(TrvOrder order) {
        var bearer = getBearer();
        Order orderToPlace;
        if (order.activation() == null) {
            orderToPlace = Order.newBuilder()
                    .setSymbol(order.tickerCode())
                    .setAccountId(tradevisorProperties.integration().finam().accountId())
                    .setSide(order.direction() > 0 ? Side.SIDE_BUY : Side.SIDE_SELL)
                    .setType(OrderType.ORDER_TYPE_LIMIT)
                    .setTimeInForce(TIME_IN_FORCE_GOOD_TILL_CANCEL)
                    .setLimitPrice(Decimal.newBuilder()
                            .setValue(String.valueOf(order.price()))
                            .build())
                    .build();
        } else {
            orderToPlace = Order.newBuilder()
                    .setSymbol(order.tickerCode())
                    .setAccountId(tradevisorProperties.integration().finam().accountId())
                    .setSide(order.direction() > 0 ? Side.SIDE_BUY : Side.SIDE_SELL)
                    .setType(OrderType.ORDER_TYPE_STOP_LIMIT)
                    .setTimeInForce(TIME_IN_FORCE_GOOD_TILL_CANCEL)
                    // если передан activation параметр значит это ордер на stop loss. для лонга SL < текущей цены, для шорта - наоборот
                    .setStopCondition(order.direction() > 0 ? StopCondition.STOP_CONDITION_LAST_DOWN : StopCondition.STOP_CONDITION_LAST_UP)
                    .setStopPrice(Decimal.newBuilder()
                            .setValue(String.valueOf(order.price()))
                            .build())
                    .setLimitPrice(Decimal.newBuilder()
                            .setValue(String.valueOf(order.price()))
                            .build())
                    .build();
        }

        var res = ordersServiceBlockingStub
                .withCallCredentials(bearer)
                .placeOrder(orderToPlace);
        log.info("ордер выставлен : {}", res.toString());
    }


    @Override
    public void deleteOrders(String tickerCode) {
        var bearer = getBearer();
        OrdersResponse orders = ordersServiceBlockingStub.withCallCredentials(bearer)
                .getOrders(OrdersRequest.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .build());
        var cancelResult = orders.getOrdersList().stream().map(o -> ordersServiceBlockingStub
                        .cancelOrder(CancelOrderRequest.newBuilder()
                                .setAccountId(tradevisorProperties.integration().finam().accountId())
                                .setOrderId(o.getOrderId())
                                .build()))
                .toList();
        var notCancelledOrders = cancelResult.stream().filter(o -> o.getStatus() != OrderStatus.ORDER_STATUS_CANCELED)
                .toList();
        if (!notCancelledOrders.isEmpty()) {
            log.error("не удалось отменить ордера : {}", notCancelledOrders.toString());
            throw new IllegalStateException();
        }
    }

    @Override
    public Boolean closeAll() {
        var bearer = getBearer();
        ordersServiceBlockingStub
                .withCallCredentials(bearer)
                .getOrders(OrdersRequest.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .build())
                .getOrdersList()
                .stream().map(o -> o.getOrder().getClientOrderId())
                .forEach(id -> ordersServiceBlockingStub
                        .withCallCredentials(bearer)
                        .cancelOrder(CancelOrderRequest.newBuilder()
                                .setAccountId(tradevisorProperties.integration().finam().accountId())
                                .setOrderId(id)
                                .build())
                );
        return true;
    }

    @Override
    public boolean openPosition(Signals signal) {
        Tickers signalTicker = tickersRepository.getTickerByTickerCode(signal.getTickerCode());
        Tickers tradeTicker = tickersRepository.findTradeTickerByTickerCode(signalTicker.getTickerCode());
        if (tradeTicker == null) {
            log.warn("торговый тикер не выставлен. открытие только вручную. SignalId: {}, tickerCode: {}, direction: {}",
                    signal.getId(), signal.getTickerCode(), signal.getDirection());
            return false;
        }
        double go = NANOS_DIGITS * tradeTicker.getGo();
        Money balanceMoney = accountsServiceBlockingStub.withCallCredentials(getBearer())
                .getAccount(GetAccountRequest.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .build())
                .getCashList()
                .stream().findFirst()
                .orElseThrow();
        double balance = balanceMoney.getUnits() + NANOS_DIGITS * balanceMoney.getNanos();
        double maxRiskInMoney = balance * tradevisorProperties.trade().limits() / 100;
        double availableLots = (balance - maxRiskInMoney) / (signal.getPriceOpen() * go);
        double stopLossInMoneyFor1Lot = Math.abs(signal.getStopLoss() - signal.getPriceOpen());
        double countedRiskLots = maxRiskInMoney / stopLossInMoneyFor1Lot;
        int tradeLots = (int) Math.floor(Math.min(availableLots, countedRiskLots));
        if (tradeLots == 0) {
            log.warn("недостаточно денег для открытия позиции. Signal: {}. ticker: {}", signal.getId(), signal.getTickerCode());
            return false;
        }
        var bearer = getBearer();
        var openPositionOrderResult = ordersServiceBlockingStub.withCallCredentials(bearer)
                .placeOrder(Order.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .setSymbol(signal.getTickerCode())
                        .setType(OrderType.ORDER_TYPE_LIMIT)
                        .setLimitPrice(Decimal.newBuilder()
                                .setValue(BigDecimal.valueOf(signal.getPriceOpen()).toString())
                                .build())
                        .setTimeInForce(TIME_IN_FORCE_GOOD_TILL_CANCEL)
                        .setQuantity(Decimal.newBuilder()
                                .setValue(String.valueOf(tradeLots))
                                .build())
                        .setSide(signal.getDirection() > 0 ? Side.SIDE_BUY : Side.SIDE_SELL)
                        .build());
        OrderState stopLossOrder = ordersServiceBlockingStub.withCallCredentials(bearer)
                .placeOrder(Order.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .setSymbol(signal.getTickerCode())
                        .setType(OrderType.ORDER_TYPE_STOP)
                        .setLimitPrice(Decimal.newBuilder()
                                .setValue(BigDecimal.valueOf(signal.getStopLoss()).toString())
                                .build())
                        .setStopPrice(Decimal.newBuilder()
                                .setValue(BigDecimal.valueOf(signal.getStopLoss()).toString())
                                .build())
                        .setStopCondition(signal.getDirection() > 0
                                ? StopCondition.STOP_CONDITION_LAST_DOWN
                                : StopCondition.STOP_CONDITION_LAST_UP)
                        .setTimeInForce(TIME_IN_FORCE_GOOD_TILL_CANCEL)
                        .setQuantity(Decimal.newBuilder()
                                .setValue(String.valueOf(tradeLots))
                                .build())
                        .setSide(signal.getDirection() > 0 ? Side.SIDE_SELL : Side.SIDE_BUY)
                        .build());
        OrderState takeProfitOrder = ordersServiceBlockingStub.withCallCredentials(bearer)
                .placeOrder(Order.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .setSymbol(signal.getTickerCode())
                        .setType(OrderType.ORDER_TYPE_LIMIT)
                        .setLimitPrice(Decimal.newBuilder()
                                .setValue(BigDecimal.valueOf(signal.getTakeProfit()).toString())
                                .build())
                        .setTimeInForce(TIME_IN_FORCE_GOOD_TILL_CANCEL)
                        .setQuantity(Decimal.newBuilder()
                                .setValue(String.valueOf(tradeLots))
                                .build())
                        .setSide(signal.getDirection() > 0 ? Side.SIDE_SELL : Side.SIDE_BUY)
                        .build());
        if (!openPositionOrderResult.hasAcceptAt() || !stopLossOrder.hasAcceptAt() || !takeProfitOrder.hasAcceptAt()) {
            log.error("ордер не выставлен. необходима проверка вручную. SignalId: {}. TickerId: {}. Direction: {}",
                    signal.getId(), signal.getTickerCode(), signal.getDirection());
            throw new IllegalStateException();
        } else {
            log.info("позиция выставлена успешно.SignalId: {}. TickerId: {}. Direction: {}",
                    signal.getId(), signal.getTickerCode(), signal.getDirection());
            return true;
        }
    }
}