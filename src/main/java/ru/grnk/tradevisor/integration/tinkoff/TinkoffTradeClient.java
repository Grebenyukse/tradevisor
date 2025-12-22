package ru.grnk.tradevisor.integration.tinkoff;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.entity.Signals;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.grnk.tradevisor.trade.TradeClient;
import ru.grnk.tradevisor.trade.dto.TrvOrder;
import ru.grnk.tradevisor.trade.dto.TrvPosition;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.ttech.piapi.core.helpers.NumberMapper;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static ru.grnk.tradevisor.collect.prices.BindTradeFuturesService.TRV_PROVIDER_TINKOFF;


@RequiredArgsConstructor
@Slf4j
@Service
@ConditionalOnProperty(value = "app.integration.tinkoff.enabled")
public class TinkoffTradeClient implements TradeClient {

    public static final double NANOS_DIGITS = Math.pow(10, -9);
    private final InvestApi investApi;
    private final TradevisorProperties tradevisorProperties;
    private final TickersRepository tickersRepository;


    private String tradingAccountId;

    @PostConstruct
    public void init() {
        var accountsResponse = investApi.getUserService().getAccountsSync(AccountStatus.ACCOUNT_STATUS_OPEN);
        tradingAccountId = accountsResponse.stream()
                .filter(acc -> acc.getType() == AccountType.ACCOUNT_TYPE_TINKOFF)
                .findFirst()
                .map(Account::getId)
                .orElseThrow(() -> new IllegalStateException("Не найден открытый брокерский счет"));
        log.info("Брокерский счет: {}", tradingAccountId);
    }

    @Override
    public String provider() {
        return TRV_PROVIDER_TINKOFF;
    }

    public Float getBalance() {
        var margin = investApi.getUserService().getMarginAttributesSync(tradingAccountId).getCorrectedMargin();
        return (float) (margin.getUnits() + NANOS_DIGITS * margin.getNano());
    }

    @Override
    public List<TrvOrder> getOrdersByTicker(String tickerCode) {
        var orders = investApi.getOrdersService().getOrdersSync(tradingAccountId)
                .stream()
                .map(x -> TrvOrder.builder()
                        .tickerCode(x.getInstrumentUid())
                        .lot(x.getLotsRequested())
                        .isGtc(true)
                        .direction(x.getDirectionValue() == 1 ? 1 : -1)
                        .price(moneyToFloat(x.getInitialOrderPrice()))
                        .status(x.hasExecutedOrderPrice() ? "executed" : "pending")
                        .build())
                .toList();
        var stopOrdersService = investApi.getStopOrdersService().getStopOrdersSync(tradingAccountId)
                .stream()
                .map(x -> TrvOrder
                        .builder()
                        .tickerCode(x.getInstrumentUid())
                        .activation(moneyToFloat(x.getStopPrice()))
                        .price(moneyToFloat(x.getPrice()))
                        .lot(x.getLotsRequested())
                        .isGtc(!x.hasExpirationTime())
                        .direction(x.getDirectionValue() == 1 ? 1 : -1)
                        .build()
                ).toList();
        return Stream.concat(orders.stream(), stopOrdersService.stream())
                .filter(x -> Objects.equals(x.tickerCode(), tickerCode))
                .collect(toList());
    }

    @Override
    public TrvPosition getAvgPositionByTicker(String tickerCode) {
        List<TrvOrder> orders = this.getOrdersByTicker(tickerCode);
        Float tp = orders.stream().filter(x -> x.activation() == null).findFirst().map(TrvOrder::price).orElse(null);
        Float sl = orders.stream().filter(x -> x.activation() != null).findFirst().map(TrvOrder::price).orElse(null);
        return investApi.getOrdersService().getOrdersSync(tradingAccountId)
                .stream()
                .filter(o -> o.getInstrumentUid().equals(tickerCode))
                .findFirst()
                .map(x -> TrvPosition.builder()
                        .tickerCode(tickerCode)
                        .price(moneyToFloat(x.getAveragePositionPrice()))
                        .sl(sl)
                        .tp(tp)
                        .lot((float) x.getLotsRequested())
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
                    tradingAccountId,
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
                    tradingAccountId,
                    StopOrderType.STOP_ORDER_TYPE_STOP_LIMIT
            );
            log.info("Ордер STOP LOSS выставлен. orderId: {}", orderId);
            return orderId;
        }
    }

    @Override
    public void deleteOrders(String tickerCode) {
        investApi.getOrdersService().getOrdersSync(tradingAccountId)
                .stream()
                .filter(x -> x.getInstrumentUid().equals(tickerCode))
                .forEach(x -> investApi.getOrdersService()
                        .cancelOrderSync(tradingAccountId, x.getOrderId()));
        investApi.getStopOrdersService().getStopOrdersSync(tradingAccountId)
                .stream()
                .filter(x -> x.getInstrumentUid().equals(tickerCode))
                .forEach(x -> investApi.getStopOrdersService()
                        .cancelStopOrderSync(tradingAccountId, x.getStopOrderId()));
    }

    @Override
    public boolean openPosition(Signals rawSignal) {
        Signals signal = mapSignalToTradeTicker(rawSignal);
        InstrumentShort tInstrument = investApi.getInstrumentsService()
                .findInstrumentSync(signal.getTickerCode())
                .stream()
                .findFirst()
                .orElseThrow();
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

    private float getSpot2TradeTickerK(String spotTickerCode, String tradeTickerCode) {
        var lastPrices = investApi.getMarketDataService().getLastPricesSync(List.of(tradeTickerCode, spotTickerCode))
                .stream()
                .map(x -> quotationToFloat(x.getPrice()))
                .toList();
        if (lastPrices.size() != 2) {
            throw new IllegalStateException();
        }
        return spotTickerCode.equals(tradeTickerCode) ? 1 : lastPrices.get(1) / lastPrices.get(0);
    }

    private Signals mapSignalToTradeTicker(Signals rawSignal) {
        Tickers spotTicker = tickersRepository.getTickerByTickerCode(rawSignal.getTickerCode());
        Tickers tradeTicker = tickersRepository.findTradeTickerByTickerCodeIfExists(spotTicker.getTickerCode())
                .orElse(spotTicker);
        float kFut2Spot = getSpot2TradeTickerK(spotTicker.getTickerCode(), tradeTicker.getTradeTickerCode());
        var minPriceIncrement = getMinPriceIncrement(tradeTicker.getTickerCode());
        return Signals.builder()
                .priceOpen(roundPrice(rawSignal.getPriceOpen() * kFut2Spot, minPriceIncrement, rawSignal.getDirection()))
                .stopLoss(roundPrice(rawSignal.getStopLoss() * kFut2Spot, minPriceIncrement, rawSignal.getDirection()))
                .takeProfit(roundPrice(rawSignal.getTakeProfit() * kFut2Spot, minPriceIncrement, rawSignal.getDirection()))
                .id(rawSignal.getId())
                .createdAt(rawSignal.getCreatedAt())
                .description(rawSignal.getDescription())
                .direction(rawSignal.getDirection())
                .name(rawSignal.getName())
                .status(rawSignal.getStatus())
                .strategyProps(rawSignal.getStrategyProps())
                .tickerCode(rawSignal.getTickerCode())
                .updatedAt(rawSignal.getUpdatedAt())
                .build();
    }

    @NotNull
    private Float roundPrice(Float price, BigDecimal minPriceIncrement, int direction) {
        return direction > 0
                ? roundDownPrice(BigDecimal.valueOf((double) price), minPriceIncrement)
                : roundUpPrice(BigDecimal.valueOf((double) price), minPriceIncrement);
    }

    private boolean openFuturePosition(Signals signal) {
        Future future = investApi.getInstrumentsService().getFutureByUidSync(signal.getTickerCode());
        int tradeLots = countTradeLots(signal, future);
        if (tradeLots == 0) {
            log.warn("недостаточно денег для открытия позиции по фьючерсам. Signal: {}", signal);
            return false;
        }
        var positionOrderResp = investApi.getOrdersService()
                .postLimitOrderSync(
                        signal.getTickerCode(),
                        (long) tradeLots,
                        quotationFromFloat(signal.getPriceOpen()),
                        signal.getDirection() > 0
                                ? OrderDirection.ORDER_DIRECTION_BUY
                                : OrderDirection.ORDER_DIRECTION_SELL,
                        tradingAccountId,
                        TimeInForceType.TIME_IN_FORCE_DAY,
                        UUID.randomUUID().toString()
                );
        if (!isOrderAccepted(investApi.getOrdersService()
                .getOrderStateSync(tradingAccountId, positionOrderResp.getOrderId())
                .getExecutionReportStatus())
        ) {
            investApi.getOrdersService().cancelOrderSync(tradingAccountId, positionOrderResp.getOrderId());
            return false;
        }
        var stopLossOrderId = this.setOrder(TrvOrder.builder()
                        .lot(tradeLots)
                        .activation(signal.getStopLoss())
                        .price(signal.getStopLoss())
                        .tickerCode(signal.getTickerCode())
                        .direction(signal.getDirection() * -1)  // сигнал на закрытие противоположный открытию
                        .isGtc(true)
                .build());
        if (!isStopOrderAccepted(investApi.getStopOrdersService()
                .getStopOrdersSync(tradingAccountId)
                .stream()
                .filter(o -> o.getStopOrderId().equals(stopLossOrderId))
                .findFirst()
                .orElseThrow().getStatus()
        )) {
            this.deleteOrders(signal.getTickerCode());
            return false;
        }
        var takeProfitOrderId = this.setOrder(TrvOrder.builder()
                        .lot(tradeLots)
                        .activation(signal.getTakeProfit())
                        .price(signal.getTakeProfit())
                        .tickerCode(signal.getTickerCode())
                        .direction(signal.getDirection() * -1) // сигнал на закрытие противоположный открытию
                        .isGtc(true)
                .build());
        if (!isStopOrderAccepted(investApi.getStopOrdersService()
                .getStopOrdersSync(tradingAccountId)
                .stream()
                .filter(o -> o.getStopOrderId().equals(takeProfitOrderId))
                .findFirst()
                .orElseThrow().getStatus()
        )) {
            this.deleteOrders(signal.getTickerCode());
            return false;
        }
        return true;
    }

    private boolean openSharePosition(Signals signal) {
        Share share = investApi.getInstrumentsService().getShareByUidSync(signal.getTickerCode());
        if (!share.getSellAvailableFlag() && signal.getDirection() < 0) {
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
                        signal.getTickerCode(),
                        (long) tradeLots,
                        quotationFromFloat(signal.getPriceOpen()),
                        signal.getDirection() > 0
                                ? OrderDirection.ORDER_DIRECTION_BUY
                                : OrderDirection.ORDER_DIRECTION_SELL,
                        tradingAccountId,
                        TimeInForceType.TIME_IN_FORCE_DAY,
                        UUID.randomUUID().toString()
                );
        if (!isOrderAccepted(investApi.getOrdersService()
                .getOrderStateSync(tradingAccountId, positionOrderResp.getOrderId())
                .getExecutionReportStatus())
        ) {
            investApi.getOrdersService().cancelOrderSync(tradingAccountId, positionOrderResp.getOrderId());
            return false;
        }
        var stopLossOrderId = this.setOrder(TrvOrder.builder()
                .lot(tradeLots)
                .activation(signal.getStopLoss())
                .price(signal.getStopLoss())
                .tickerCode(signal.getTickerCode())
                .direction(signal.getDirection() * -1)  // сигнал на закрытие противоположный открытию
                .isGtc(true)
                .build());
        if (!isStopOrderAccepted(investApi.getStopOrdersService()
                .getStopOrdersSync(tradingAccountId)
                .stream()
                .filter(o -> o.getStopOrderId().equals(stopLossOrderId))
                .findFirst()
                .orElseThrow().getStatus()
        )) {
            this.deleteOrders(signal.getTickerCode());
            return false;
        }
        var takeProfitOrderId = this.setOrder(TrvOrder.builder()
                .lot(tradeLots)
                .activation(signal.getTakeProfit())
                .price(signal.getTakeProfit())
                .tickerCode(signal.getTickerCode())
                .direction(signal.getDirection() * -1) // сигнал на закрытие противоположный открытию
                .isGtc(true)
                .build());
        if (!isStopOrderAccepted(investApi.getStopOrdersService()
                .getStopOrdersSync(tradingAccountId)
                .stream()
                .filter(o -> o.getStopOrderId().equals(takeProfitOrderId))
                .findFirst()
                .orElseThrow().getStatus()
        )) {
            this.deleteOrders(signal.getTickerCode());
            return false;
        }
        return true;
    }


    private Quotation quotationFromFloat(Float value) {
        if (value == null) {
            return Quotation.newBuilder().setUnits(0).setNano(0).build();
        }
        long units = value.longValue();
        int nanos = (int) Math.round((value - units) * 1_000_000_000);
        // Обработка отрицательных значений
        if (value < 0 && nanos != 0) {
            units -= 1; // Корректируем units для отрицательных чисел
            nanos += 1_000_000_000; // Делаем nanos положительным
        }
        return Quotation
                .newBuilder()
                .setUnits(units)
                .setNano(nanos)
                .build();
    }

    private Float quotationToFloat(Quotation quotation) {
        return (float) (quotation.getUnits() + NANOS_DIGITS * quotation.getNano());
    }

    private Float moneyToFloat(MoneyValue money) {
        return (float) (money.getUnits() + NANOS_DIGITS * money.getNano());
    }

    private BigDecimal getMinPriceIncrement(String instrumentId) {
        return NumberMapper.quotationToBigDecimal(investApi.getInstrumentsService()
                .getInstrumentByUIDSync(instrumentId).getInstrument().getMinPriceIncrement());
    }

    private Float roundUpPrice(BigDecimal price, BigDecimal minPriceIncrement) {
        return price.divide(minPriceIncrement, 0, RoundingMode.UP)
                .multiply(minPriceIncrement)
                .floatValue();
    }

    private Float roundDownPrice(BigDecimal price, BigDecimal minPriceIncrement) {
        return price.divide(minPriceIncrement, 0, RoundingMode.DOWN)
                .multiply(minPriceIncrement).floatValue();
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

    private int countTradeLots(Signals signal, Share share) {
        return countTradeLotsForGo(signal, share.getUid(), 1);
    }

    private int countTradeLots(Signals signal, Future future) {
        float go = quotationToFloat(signal.getDirection() > 0 ? future.getDlongMin() : future.getDshortMin());
        return countTradeLotsForGo(signal, future.getUid(), go);
    }

    private int countTradeLotsForGo(Signals signal, String  uid, float go) {
        float balance = this.getBalance();
        double maxRiskInMoney = balance * tradevisorProperties.trade().limits() / 100;
        var maxLotsResp = investApi.getOrdersService().getMaxLotsSync(tradingAccountId, uid,
                quotationFromFloat(signal.getPriceOpen()));
        int maxLots = signal.getDirection() > 0
                ? (int) maxLotsResp.getBuyLimits().getBuyMaxLots()
                : (int) maxLotsResp.getSellLimits().getSellMaxLots();
        double availableLots = (balance - maxRiskInMoney) / (signal.getPriceOpen() * go);
        double stopLossInMoneyFor1Lot = Math.abs(signal.getStopLoss() - signal.getPriceOpen());
        double countedRiskLots = maxRiskInMoney / stopLossInMoneyFor1Lot;
        return (int) (Math.floor(
                Math.min(
                        Math.min(availableLots, countedRiskLots),
                        (double) maxLots * (1 - tradevisorProperties.trade().limits()) / 100
                )
        ));
    }
}
