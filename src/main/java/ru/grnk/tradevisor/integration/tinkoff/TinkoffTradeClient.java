package ru.grnk.tradevisor.integration.tinkoff;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.entity.Signals;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.grnk.tradevisor.integration.tinkoff.dto.TradeSignal;
import ru.grnk.tradevisor.trade.TradeClient;
import ru.grnk.tradevisor.trade.dto.TrvOrder;
import ru.grnk.tradevisor.trade.dto.TrvPosition;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.models.Money;
import ru.ttech.piapi.core.helpers.NumberMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static ru.grnk.tradevisor.collect.prices.BindTradeFuturesService.TRV_PROVIDER_TINKOFF;
import static ru.grnk.tradevisor.common.util.RoundPriceUtils.moneyToBigDecimal;
import static ru.grnk.tradevisor.common.util.RoundPriceUtils.roundPrice;
import static ru.tinkoff.piapi.core.models.Quantity.NANOS_MULTIPLIER;


@RequiredArgsConstructor
@Slf4j
@Service
@ConditionalOnProperty(value = "app.integration.tinkoff.enabled")
public class TinkoffTradeClient implements TradeClient {

    public static final double NANOS_DIGITS = Math.pow(10, -9);
    private final InvestApi investApi;
    private final TradevisorProperties tradevisorProperties;
    private final TickersRepository tickersRepository;

    private final AtomicReference<String> tradingAccountId = new AtomicReference<>();

    private void init() {
        if (tradingAccountId.get() != null) return;
        var accountsResponse = investApi.getUserService().getAccountsSync(AccountStatus.ACCOUNT_STATUS_OPEN);
        String accountId = accountsResponse.stream()
                .filter(acc -> acc.getType() == AccountType.ACCOUNT_TYPE_TINKOFF)
                .findFirst()
                .map(Account::getId)
                .orElseThrow(() -> new IllegalStateException("Не найден открытый брокерский счет"));
        if (tradingAccountId.compareAndSet(null, accountId)) {
            log.info("Брокерский счет: {}", accountId);
        }
    }

    private String getTradingAccountId() {
        init();
        return tradingAccountId.get();
    }

    @Override
    public void checkPositionStatus(String tickerCode) {
        log.info("check position status completed");
    }

    @Override
    public boolean isPositionOpened(String tickerCode) {
        return this.getAvgPositionByTicker(tickerCode) != null;
    }

    @Override
    public String provider() {
        return TRV_PROVIDER_TINKOFF;
    }

    public Float getBalance() {
        return investApi.getOperationsService().getWithdrawLimitsSync(tradingAccountId.get()).getMoney()
                .stream().filter(x -> Objects.equals(x.getCurrency(), "rub"))
                .findFirst()
                .map(Money::getValue)
                .map(BigDecimal::floatValue)
                .orElseThrow();
    }

    @Override
    public List<TrvOrder> getOrdersByTicker(String tickerCode) {
        var orders = investApi.getOrdersService().getOrdersSync(getTradingAccountId())
                .stream()
                .map(x -> TrvOrder.builder()
                        .tickerCode(x.getInstrumentUid())
                        .lot((int) x.getLotsRequested())
                        .isGtc(true)
                        .direction(x.getDirectionValue() == 1 ? 1 : -1)
                        .price(moneyToBigDecimal(x.getInitialOrderPrice()))
                        .status(x.hasExecutedOrderPrice() ? "executed" : "pending")
                        .build())
                .toList();
        var stopOrdersService = investApi.getStopOrdersService().getStopOrdersSync(getTradingAccountId())
                .stream()
                .map(x -> TrvOrder
                        .builder()
                        .tickerCode(x.getInstrumentUid())
                        .activation(moneyToBigDecimal(x.getStopPrice()))
                        .price(moneyToBigDecimal(x.getPrice()))
                        .lot((int) x.getLotsRequested())
                        .isGtc(!x.hasExpirationTime())
                        .direction(x.getDirectionValue() == 1 ? 1 : -1)
                        .build()
                ).toList();
        return Stream.concat(orders.stream(), stopOrdersService.stream())
                .filter(x -> Objects.equals(x.tickerCode(), tickerCode))
                .collect(toList());
    }

    public TrvPosition getAvgPositionByTicker(String tickerCode) {
        List<TrvOrder> orders = this.getOrdersByTicker(tickerCode);
        BigDecimal tp = orders.stream().filter(x -> x.activation() == null).findFirst().map(TrvOrder::price).orElse(null);
        BigDecimal sl = orders.stream().filter(x -> x.activation() != null).findFirst().map(TrvOrder::price).orElse(null);
        return investApi.getOrdersService().getOrdersSync(getTradingAccountId())
                .stream()
                .filter(o -> o.getInstrumentUid().equals(tickerCode))
                .findFirst()
                .map(x -> TrvPosition.builder()
                        .tickerCode(tickerCode)
                        .price(moneyToBigDecimal(x.getAveragePositionPrice()))
                        .sl(sl)
                        .tp(tp)
                        .lot((int) x.getLotsRequested())
                        .direction(x.getDirectionValue() == 1 ? 1 : -1)
                        .build())
                .orElse(null);
    }

    public String setOrder(TrvOrder order) {
        if (order.activation() == null) {
            var resp = investApi.getOrdersService().postLimitOrderSync(
                    order.tickerCode(),
                    (long) order.lot(),
                    quotationFromFloat(order.price()),
                    order.direction() > 0 ? OrderDirection.ORDER_DIRECTION_BUY : OrderDirection.ORDER_DIRECTION_SELL,
                    getTradingAccountId(),
                    TimeInForceType.TIME_IN_FORCE_DAY,
                    UUID.randomUUID().toString()
            );
            log.info("Ордер выставлен: {}", resp);
            return resp.getOrderId();
        } else {
            String orderId = investApi.getStopOrdersService().postStopOrderGoodTillCancelSync(
                    order.tickerCode(),
                    (long) order.lot(),
                    quotationFromFloat(order.price()),
                    quotationFromFloat(order.activation()),
                    order.direction() > 0 ? StopOrderDirection.STOP_ORDER_DIRECTION_BUY : StopOrderDirection.STOP_ORDER_DIRECTION_SELL,
                    getTradingAccountId(),
                    StopOrderType.STOP_ORDER_TYPE_STOP_LIMIT
            );
            log.info("Ордер STOP LOSS выставлен. orderId: {}", orderId);
            return orderId;
        }
    }

    @Override
    public void deleteOrders(String tickerCode) {
        investApi.getOrdersService().getOrdersSync(getTradingAccountId())
                .stream()
                .filter(x -> x.getInstrumentUid().equals(tickerCode))
                .forEach(x -> investApi.getOrdersService()
                        .cancelOrderSync(getTradingAccountId(), x.getOrderId()));
        investApi.getStopOrdersService().getStopOrdersSync(getTradingAccountId())
                .stream()
                .filter(x -> x.getInstrumentUid().equals(tickerCode))
                .forEach(x -> investApi.getStopOrdersService()
                        .cancelStopOrderSync(getTradingAccountId(), x.getStopOrderId()));
    }

    @Override
    public boolean openPosition(Signals rawSignal) {
        TradeSignal signal = mapSignalToTradeTicker(rawSignal);
        InstrumentShort tInstrument = investApi.getInstrumentsService()
                .findInstrumentSync(signal.tickerCode())
                .stream()
                .findFirst()
                .orElseThrow();
        if (tInstrument.getForQualInvestorFlag()) {
            log.warn("for qualified investors only. Manual execution only. signal: {}", signal);
            return false;
        }
        return switch (tInstrument.getInstrumentKind()) {
            case INSTRUMENT_TYPE_FUTURES -> openFuturePosition(signal);
            case INSTRUMENT_TYPE_SHARE -> openSharePosition(signal);
            default -> {
                log.info("unknown instrument type: {}. Signal: {}",
                        tInstrument.getInstrumentKind(), signal);
                yield false;
            }
        };
    }

    private float kFutBySpot(String futTickerCode, String spotTickerCode) {
        if (Objects.equals(futTickerCode, spotTickerCode)) return 1;
        var lastPrices = investApi.getMarketDataService().getLastPricesSync(List.of(futTickerCode, spotTickerCode))
                .stream()
                .map(x -> quotationToFloat(x.getPrice()))
                .toList();
        if (lastPrices.size() != 2) {
            throw new IllegalStateException();
        }
        return futTickerCode.equals(spotTickerCode) ? 1 : lastPrices.get(0) / lastPrices.get(1);
    }

    private TradeSignal mapSignalToTradeTicker(Signals rawSignal) {
        Tickers spotTicker = tickersRepository.getTickerByTickerCode(rawSignal.getTickerCode());
        Tickers tradeTicker = tickersRepository.findTradeTickerByTickerCodeIfExists(spotTicker.getTickerCode())
                .orElse(spotTicker);
        float kFut2Spot = kFutBySpot(tradeTicker.getTickerCode(), spotTicker.getTickerCode());
        var minPriceIncrement = getMinPriceIncrement(tradeTicker.getTickerCode());
        return TradeSignal.builder()
                .priceOpen(roundPrice(rawSignal.getPriceOpen() * kFut2Spot, minPriceIncrement, rawSignal.getDirection()))
                .stopLoss(roundPrice(rawSignal.getStopLoss() * kFut2Spot, minPriceIncrement, rawSignal.getDirection()))
                .takeProfit(roundPrice(rawSignal.getTakeProfit() * kFut2Spot, minPriceIncrement, rawSignal.getDirection()))
                .id(rawSignal.getId())
                .createdAt(rawSignal.getCreatedAt())
                .description(rawSignal.getDescription())
                .direction(rawSignal.getDirection())
                .name(rawSignal.getName())
                .status(rawSignal.getStatus())
                .tickerCode(rawSignal.getTickerCode())
                .updatedAt(rawSignal.getUpdatedAt())
                .build();
    }

        private boolean openFuturePosition(TradeSignal signal) {
        Future future = investApi.getInstrumentsService().getFutureByUidSync(signal.tickerCode());
        int tradeLots = countTradeLots(signal, future);
        if (tradeLots == 0) {
            log.warn("недостаточно денег для открытия позиции по фьючерсам. Signal: {}", signal);
            return false;
        }
        var positionOrderResp = investApi.getOrdersService()
                .postLimitOrderSync(
                        signal.tickerCode(),
                        (long) tradeLots,
                        quotationFromFloat(signal.priceOpen()),
                        signal.direction() > 0
                                ? OrderDirection.ORDER_DIRECTION_BUY
                                : OrderDirection.ORDER_DIRECTION_SELL,
                        getTradingAccountId(),
                        TimeInForceType.TIME_IN_FORCE_DAY,
                        UUID.randomUUID().toString()
                );
        if (!isOrderAccepted(investApi.getOrdersService()
                .getOrderStateSync(getTradingAccountId(), positionOrderResp.getOrderId())
                .getExecutionReportStatus())
        ) {
            investApi.getOrdersService().cancelOrderSync(getTradingAccountId(), positionOrderResp.getOrderId());
            return false;
        }
        var stopLossOrderId = this.setOrder(TrvOrder.builder()
                        .lot(tradeLots)
                        .activation(signal.stopLoss())
                        .price(signal.stopLoss())
                        .tickerCode(signal.tickerCode())
                        .direction(signal.direction() * -1)  // сигнал на закрытие противоположный открытию
                        .isGtc(true)
                .build());
        if (!isStopOrderAccepted(investApi.getStopOrdersService()
                .getStopOrdersSync(getTradingAccountId())
                .stream()
                .filter(o -> o.getStopOrderId().equals(stopLossOrderId))
                .findFirst()
                .orElseThrow().getStatus()
        )) {
            this.deleteOrders(signal.tickerCode());
            return false;
        }
        var takeProfitOrderId = this.setOrder(TrvOrder.builder()
                        .lot(tradeLots)
                        .activation(signal.takeProfit())
                        .price(signal.takeProfit())
                        .tickerCode(signal.tickerCode())
                        .direction(signal.direction() * -1) // сигнал на закрытие противоположный открытию
                        .isGtc(true)
                .build());
        if (!isStopOrderAccepted(investApi.getStopOrdersService()
                .getStopOrdersSync(getTradingAccountId())
                .stream()
                .filter(o -> o.getStopOrderId().equals(takeProfitOrderId))
                .findFirst()
                .orElseThrow().getStatus()
        )) {
            this.deleteOrders(signal.tickerCode());
            return false;
        }
        return true;
    }

    private boolean openSharePosition(TradeSignal signal) {
        Share share = investApi.getInstrumentsService().getShareByUidSync(signal.tickerCode());
        if (!share.getBuyAvailableFlag()) {
            log.warn("запрещена покупка актива. signal: {}", signal);
            return false;
        }
        if (!share.getSellAvailableFlag() && signal.direction() < 0) {
            log.warn("запрещена продажа без покрытия. signal: {}", signal);
            return false;
        }
        int tradeLots = countTradeLots(signal, share);
        if (tradeLots == 0) {
            log.warn("недостаточно денег для открытия позиции. Signal: {}", signal);
            return false;
        }
        var positionOrderResp = investApi.getOrdersService()
                .postLimitOrderSync(
                        signal.tickerCode(),
                        (long) tradeLots,
                        quotationFromFloat(signal.priceOpen()),
                        signal.direction() > 0
                                ? OrderDirection.ORDER_DIRECTION_BUY
                                : OrderDirection.ORDER_DIRECTION_SELL,
                        getTradingAccountId(),
                        TimeInForceType.TIME_IN_FORCE_DAY,
                        UUID.randomUUID().toString()
                );
        if (!isOrderAccepted(investApi.getOrdersService()
                .getOrderStateSync(getTradingAccountId(), positionOrderResp.getOrderId())
                .getExecutionReportStatus())
        ) {
            investApi.getOrdersService().cancelOrderSync(getTradingAccountId(), positionOrderResp.getOrderId());
            return false;
        }
        var stopLossOrderId = this.setOrder(TrvOrder.builder()
                .lot(tradeLots)
                .activation(signal.stopLoss())
                .price(signal.stopLoss())
                .tickerCode(signal.tickerCode())
                .direction(signal.direction() * -1)  // сигнал на закрытие противоположный открытию
                .isGtc(true)
                .build());
        if (!isStopOrderAccepted(investApi.getStopOrdersService()
                .getStopOrdersSync(getTradingAccountId())
                .stream()
                .filter(o -> o.getStopOrderId().equals(stopLossOrderId))
                .findFirst()
                .orElseThrow().getStatus()
        )) {
            this.deleteOrders(signal.tickerCode());
            return false;
        }
        var takeProfitOrderId = this.setOrder(TrvOrder.builder()
                .lot(tradeLots)
                .activation(signal.takeProfit())
                .price(signal.takeProfit())
                .tickerCode(signal.tickerCode())
                .direction(signal.direction() * -1) // сигнал на закрытие противоположный открытию
                .isGtc(true)
                .build());
        if (!isStopOrderAccepted(investApi.getStopOrdersService()
                .getStopOrdersSync(getTradingAccountId())
                .stream()
                .filter(o -> o.getStopOrderId().equals(takeProfitOrderId))
                .findFirst()
                .orElseThrow().getStatus()
        )) {
            this.deleteOrders(signal.tickerCode());
            return false;
        }
        return true;
    }


    private Quotation quotationFromFloat(BigDecimal value) {
        if (value == null) {
            return Quotation.newBuilder().setUnits(0).setNano(0).build();
        }
        long units = value.longValue();
        BigDecimal fractionalPart = value.subtract(BigDecimal.valueOf(units));
        int nanos = fractionalPart.multiply(NANOS_MULTIPLIER).setScale(0, RoundingMode.DOWN).intValue();
        return Quotation
                .newBuilder()
                .setUnits(units)
                .setNano(nanos)
                .build();
    }

    public static Float quotationToFloat(Quotation quotation) {
        return (float) (quotation.getUnits() + NANOS_DIGITS * quotation.getNano());
    }



    private BigDecimal getMinPriceIncrement(String instrumentId) {
        return NumberMapper.quotationToBigDecimal(investApi.getInstrumentsService()
                .getInstrumentByUIDSync(instrumentId).getInstrument().getMinPriceIncrement());
    }

    private static BigDecimal mapUnitsAndNanos(long units, int nanos) {
        if (units == 0 && nanos == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(units).add(BigDecimal.valueOf(nanos, 9));
    }



    private boolean isOrderAccepted(OrderExecutionReportStatus orderExecutionStatus) {
        return List.of(OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_FILL,
                        OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_NEW,
                        OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_PARTIALLYFILL)
                .contains(orderExecutionStatus);
    }

    private boolean isStopOrderAccepted(StopOrderStatusOption status) {
        return List.of(
                StopOrderStatusOption.STOP_ORDER_STATUS_ACTIVE,
                StopOrderStatusOption.STOP_ORDER_STATUS_EXECUTED
        ).contains(status);
    }

    private int countTradeLots(TradeSignal signal, Share share) {
        return countTradeLotsForGo(signal, share.getUid(), 1);
    }

    private int countTradeLots(TradeSignal signal, Future future) {
        float go = quotationToFloat(signal.direction() > 0 ? future.getDlongMin() : future.getDshortMin());
        return countTradeLotsForGo(signal, future.getUid(), go);
    }

    private int countTradeLotsForGo(TradeSignal signal, String  uid, float go) {
        float balance = this.getBalance();
        double maxRiskInMoney = balance * tradevisorProperties.trade().limits() / 100;
        var maxLotsResp = investApi.getOrdersService().getMaxLotsSync(getTradingAccountId(), uid,
                quotationFromFloat(signal.priceOpen()));
        int maxLots = signal.direction() > 0
                ? (int) maxLotsResp.getBuyLimits().getBuyMaxLots()
                : (int) maxLotsResp.getSellLimits().getSellMaxLots();
        double availableLots = (balance - maxRiskInMoney) / (signal.priceOpen().doubleValue() * go);
        double stopLossInMoneyFor1Lot = Math.abs(signal.stopLoss().doubleValue() - signal.priceOpen().doubleValue());
        double countedRiskLots = maxRiskInMoney / stopLossInMoneyFor1Lot;
        return (int) (Math.floor(
                Math.min(
                        Math.min(availableLots, countedRiskLots),
                        (double) maxLots * (1 - (double) tradevisorProperties.trade().limits() / 100)
                )
        ));
    }
}
