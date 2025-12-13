package ru.grnk.tradevisor.integration.finam;

import com.google.type.Decimal;
import grpc.tradeapi.v1.Side;
import grpc.tradeapi.v1.accounts.AccountsServiceGrpc;
import grpc.tradeapi.v1.accounts.GetAccountRequest;
import grpc.tradeapi.v1.assets.AssetsServiceGrpc;
import grpc.tradeapi.v1.marketdata.MarketDataServiceGrpc;
import grpc.tradeapi.v1.orders.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.jfree.chart.axis.Tick;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.entity.Signals;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.grnk.tradevisor.integration.rts.RtsService;
import ru.grnk.tradevisor.trade.TradeClient;
import ru.grnk.tradevisor.trade.dto.TrvOrder;
import ru.grnk.tradevisor.trade.dto.TrvPosition;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static grpc.tradeapi.v1.orders.TimeInForce.TIME_IN_FORCE_GOOD_TILL_CANCEL;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinamTradeClient implements TradeClient {

    private final TradevisorProperties tradevisorProperties;
    private final AccountsServiceGrpc.AccountsServiceBlockingStub accountsServiceBlockingStub;
    private final OrdersServiceGrpc.OrdersServiceBlockingStub ordersServiceBlockingStub;
    private final AssetsServiceGrpc.AssetsServiceBlockingStub assetsServiceBlockingStub;
    private final MarketDataServiceGrpc.MarketDataServiceBlockingStub marketDataServiceBlockingStub;
    private final FinamGrpcClientService finamGrpcClientService;
    private final TickersRepository tickersRepository;
    private final RtsService rtsService;


    @Override
    public String provider() {
        return "finam";
    }

    @Override
    public Float getBalance() {
        var bearer = finamGrpcClientService.getBearer();
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
        var bearer = finamGrpcClientService.getBearer();
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
            Tickers spotTicker = tickersRepository.findTickerByTickerCode(tickerCode);
//            Tickers tradeTicker = spotTicker.getTradeTickerCode();
            return spotTicker.getTicker();
        } catch (Exception e) {
            log.error("Error finding futures contract for spot ticker: {}", tickerCode, e);
            return null;
        }
    }

    @Override
    public Float getTickPriceForTicker(String tickerCode) {
        Tickers ticker = tickersRepository.findTickerByTickerCode(tickerCode);
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
        var bearer = finamGrpcClientService.getBearer();
        String symbol = tickerCode.replace("@finam", "");
        var orders = ordersServiceBlockingStub.withCallCredentials(bearer)
                .getOrders(OrdersRequest.newBuilder()
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .build());
        throw new NotImplementedException();
    }

    @Override
    public TrvPosition getAvgPositionByTicker(String tickerCode) {
        Tickers ticker = tickersRepository.findTickerByTickerCode(tickerCode);
        var bearer = finamGrpcClientService.getBearer();
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
                .sorted((x1, x2) -> Float.parseFloat(x2.getOrder().getLimitPrice().getValue())
                        - Float.parseFloat(x1.getOrder().getLimitPrice().getValue()) > 0 ? 1 : -1)
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
        var bearer = finamGrpcClientService.getBearer();
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
        var bearer = finamGrpcClientService.getBearer();
        OrdersResponse  orders = ordersServiceBlockingStub.withCallCredentials(bearer)
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
        var bearer = finamGrpcClientService.getBearer();
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
    public void openPosition(Signals signal) {
        Tickers signalTicker = tickersRepository.findTickerByTickerCode(signal.getTickerCode());
        Tickers tradeTicker = tickersRepository.findTickerByTickerCode(signalTicker.getTradeTickerCode());
        Float go = rtsService.getGoForFutures(tradeTicker.getTicker());
        // ...
    }

    public int calculatePositionSize(String spotTicker, float priceOpen, float stopLoss, float takeProfit) {
//            // Step 4: Determine spot ticker and exchange
//            String[] parts = spotTicker.replace("@finam", "").split("@");
//            String spotTickerCode = parts[0];
//            String exchange = parts.length > 1 ? parts[1] : "MICEX";
//
//            // Step 5: Find futures contract
//            String futuresTicker = findTickerForSpot(spotTicker);
//            if (futuresTicker == null) {
//                log.warn("No suitable futures contract found for spot ticker: {}", spotTicker);
//                return 0; // Manual trading required
//            }
//
//            // Step 6: Get guarantee provision (GO) for futures contract
//            float go = getGuaranteeProvision(futuresTicker);
//
//            // Step 7: Get current prices and calculate k-spot
//            float futuresPrice = getCurrentPrice(futuresTicker);
//            float spotPrice = getCurrentPrice(spotTickerCode);
//            float kSpot = futuresPrice / spotPrice;
//
//            // Step 8: Adjust prices with k-spot
//            float positionPriceOpen = priceOpen * kSpot;
//            float positionStopLoss = stopLoss * kSpot;
//            float positionTakeProfit = takeProfit * kSpot;
//
//            // Step 9: Calculate profit factor
//            float profitFactor = positionTakeProfit / positionStopLoss;
//
//            // Step 10: Calculate balanced profit factor price open
//            float balancedProfitFactorPriceOpen = (positionTakeProfit + positionStopLoss) / 2;
//
//            // Step 11: Select position price open
//            float selectedPriceOpen = profitFactor < 1 ? balancedProfitFactorPriceOpen : positionPriceOpen;
//
//            // Step 12: Get free money
//            float freeMoney = getFreeMargin();
//
//            // Step 13: Get target risk level
//            float targetRisk = getTargetRiskFromConfig();
//
//            // Step 14: Calculate free money after trade
//            float freeMoneyAfterTrade = freeMoney * (1 - targetRisk);
//
//            // Step 15: Calculate max lots
//            int maxLots = (int) (freeMoneyAfterTrade / go);
//
//            // Step 16: Calculate stop loss per lot in money
//            float stopLossPerLotMoney = Math.abs(selectedPriceOpen - positionStopLoss);
//
//            // Step 17: Calculate lot target with error factor
//            float errorFactor = getErrorFactorFromConfig();
//            float lotTarget = ((freeMoneyAfterTrade * (1 - targetRisk)) / go) * (1 - errorFactor);
//
//            // Step 18: Determine trade lots
//            int tradeLots = Math.min(Math.round(lotTarget), maxLots);
//
//            log.info("Position calculation: spot={}, futures={}, GO={}, freeMoney={}, maxLots={}, tradeLots={}",
//                    spotTickerCode, futuresTicker, go, freeMoney, maxLots, tradeLots);

            return 1;
    }


}