package ru.grnk.tradevisor.trade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.calculate.signals.TrvSignalStatus;
import ru.grnk.tradevisor.calculate.strategies.IStrategy;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.common.repository.SignalsRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.entity.Signals;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.grnk.tradevisor.notify.PublishSignalsService;
import ru.grnk.tradevisor.trade.dto.TrvOrder;
import ru.grnk.tradevisor.trade.dto.TrvPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.*;
import static ru.grnk.tradevisor.calculate.signals.TrvSignalStatus.CREATED;
import static ru.grnk.tradevisor.calculate.signals.TrvSignalStatus.PUBLISHED;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.trade.enabled")
public class ControlPositionService {

    private final TickersRepository tickersRepository;
    private final SignalsRepository signalsRepository;
    private final MarketDataRepository marketDataRepository;
    private final List<TradeClient> tradeClients;
    private final List<IStrategy> strategies;
    private final TradevisorProperties tradevisorProperties;
    private final PublishSignalsService publishSignalsService;

    @Scheduled(fixedRateString = "${app.trade.delay}")
    public void process() {
        List<Signals> allRelevantSignals = signalsRepository.findSignalsByStatuses(
                List.of(
                        CREATED.name(),
                        PUBLISHED.name(),
                        TrvSignalStatus.CONFIRMED.name(),
                        TrvSignalStatus.EXECUTED.name()
                )
        );
        SortedSignals result = allRelevantSignals.stream()
                .collect(groupingBy(Signals::getTickerCode))
                .values()
                .stream()
                .map(this::splitSignals)
                .reduce(
                        new SortedSignals(new ArrayList<>(), new ArrayList<>()),
                        this::mergeSortedSignals
                );
        if (!result.toCancel.isEmpty()) {
            List<Integer> idsToCancel = result.toCancel.stream()
                    .map(Signals::getId)
                    .collect(toList());
            signalsRepository.cancelExpiredSignals(idsToCancel);
        }
        result.active.forEach(
            s -> {
                switch (TrvSignalStatus.valueOf(s.getStatus())) {
                    case CREATED:
                    case PUBLISHED:
                        log.info("решение по сигналу {} не принято", s.getId());
                        return;
                    case CONFIRMED:
                        this.openPosition(s);
                        return;
                    case EXECUTED:
                        this.controlPosition(s);
                }
            }
        );
    }

    private SortedSignals splitSignals(List<Signals> signals) {
        List<Signals> sorted = signals.stream()
                .sorted(new SignalComparator())
                .collect(toList());
        List<Signals> active = List.of(sorted.get(0));
        List<Signals> toCancel = sorted.subList(1, sorted.size());
        return new SortedSignals(active, toCancel);
    }

    private SortedSignals mergeSortedSignals(SortedSignals s1, SortedSignals s2) {
        List<Signals> active = new ArrayList<>(s1.active);
        active.addAll(s2.active);
        List<Signals> toCancel = new ArrayList<>(s1.toCancel);
        toCancel.addAll(s2.toCancel);
        return new SortedSignals(active, toCancel);
    }

    private record SortedSignals(List<Signals> active, List<Signals> toCancel) { }

    public void openPosition(Signals signal) {
        Tickers ticker = tickersRepository.findTickerByTickerCode(signal.getTickerCode());
        var client = tradeClients.stream().filter(tc -> Objects.equals(tc.provider(), ticker.getProvider()))
                .findFirst()
                .orElseThrow();
        // проверяем сохранились ли предусловия для открытия позиции
        var strategy = strategies.stream().filter(s -> Objects.equals(s.getStrategyUniqueName(), signal.getName())).findFirst().orElseThrow();
        var candles = marketDataRepository.fetchMarketDataForLast(strategy.barsRequiredToCalcStrategy(), signal.getTickerCode());
        var strategyCalculationResult = strategy.calculate(candles);
        if (strategyCalculationResult.direction().directionCode() != signal.getDirection()) {
            log.info("отменен сигнал {} по причине нарушения базовых условий стратегии {}", signal.getId(), strategy.getStrategyUniqueName());
            signalsRepository.updateSignalStatus(signal.getId(), TrvSignalStatus.CANCELLED);
        }
        // проверяем есть ли открытые ордера по тикеру
        List<TrvOrder> orders = client.getOrdersByTicker(signal.getTickerCode());
        if (!orders.isEmpty()) {
            log.error("сигнал {} находится в статусе {}, но по нему есть открытые ордера {}", signal.getId(), signal.getStatus(), orders.size());
            return;
        }
        // проверяем есть ли открытые позиции по тикеру
        TrvPosition position = client.getAvgPositionByTicker(signal.getTickerCode());
        if (position != null) {
            log.error("сигнал {} находится в статусе {}, но по нему есть открытая позиция {}", signal.getId(), signal.getStatus(), position.toString());
            return;
        }
        // сигнал жив, ордеров нет, позиций нет, сигнал подтвержден пользователем -> выставляем ордера
        // 1. определяем размер лота
        client.openPosition(signal);
        // 3. меняем статус сигнала на исполнено
        signalsRepository.updateSignalStatus(signal.getId(), TrvSignalStatus.EXECUTED);
        publishSignalsService.publishOrder(signal);
    }

    private Integer getInteger(Signals signal, TradeClient client, String tickerCodeForSpot) {
        Float tickPrice = client.getTickPriceForTicker(tickerCodeForSpot);
        float stopLossMoneyPerLot = Math.abs(signal.getPriceOpen() - signal.getStopLoss()) * tickPrice;
        Integer limits = tradevisorProperties.trade().limits();
        Float lotCounted = (client.getBalance() * limits) / stopLossMoneyPerLot;
        Integer lot = Math.round(lotCounted / client.getMinLotForTicker(tickerCodeForSpot));
        if (signal.getPriceOpen() * lot < client.getFreeMargin()) {
            log.warn("Недостаточно средств для открытия позиции. SignalId: {}. TickerCodeSpot: {}, TickerCodeTrade: {}. требуется: {}. Свободно: {}",
                    signal.getId(), signal.getTickerCode(), tickerCodeForSpot,  signal.getPriceOpen()*lot, client.getFreeMargin());
            throw new RuntimeException("недостаточно средств.");
        }
        return lot;
    }

    public void controlPosition(Signals signal) {
        Tickers ticker = tickersRepository.findTickerByTickerCode(signal.getTickerCode());
        var client = tradeClients.stream().filter(tc -> Objects.equals(tc.provider(), ticker.getProvider()))
                .findFirst()
                .orElseThrow();
        var position = client.getAvgPositionByTicker(signal.getTickerCode());
        List<TrvOrder> orders = client.getOrdersByTicker(signal.getTickerCode());
        if (position == null) {
            if (orders.size() != 3) {
                log.warn("позиции нет. сигнал в статусе executed. но ордеров не 3. неверное количество ордеров для сигнала {}. удаляем все оставшиеся ордера и откатываем сигнал в статус confirmed.", signal.getId());
                client.deleteOrders(ticker.getTickerCode());
                signalsRepository.updateSignalStatus(signal.getId(), TrvSignalStatus.CONFIRMED);
            }
            // позиции нет. сигнал в статусе executed. три ордера выставлено. проверяем что сигнал не заэкспарился.
            var strategy = strategies.stream()
                    .filter(s -> Objects.equals(s.getStrategyUniqueName(), signal.getName()))
                    .findFirst()
                    .orElseThrow();
            var candles = marketDataRepository.fetchMarketDataForLast(strategy.barsRequiredToCalcStrategy(), signal.getTickerCode());
            var strategyCalculationResult = strategy.calculate(candles);
            if (strategyCalculationResult.direction().directionCode() != signal.getDirection()) {
                log.info("предпосылки торгового сигнала нарушены. удаляем ордера. сигнал переводим в стату  SignalId: {}", signal.getId());
                client.deleteOrders(ticker.getTickerCode());
            }
        } else {
            if (orders.size() != 2L) {
                log.warn("позиция выставлена. ожидается 2 ордера но их не 2. значит нет takeProfit или stopLoss. удаляем ордера и перевыставляем sl и tp заново");
                client.deleteOrders(ticker.getTickerCode());
                client.setOrder(TrvOrder
                        .builder()
                                .tickerCode(ticker.getTickerCode())
                                .isGtc(true)
                                .activation(signal.getStopLoss())
                                .price(signal.getStopLoss())
                                .direction(-1 * signal.getDirection()) //противоположно основному сигналу
                                .lot(getInteger(signal, client, signal.getTickerCode()))
                        .build());
                client.setOrder(TrvOrder
                        .builder()
                            .tickerCode(ticker.getTickerCode())
                            .isGtc(true)
                            .activation(signal.getTakeProfit())
                            .price(signal.getTakeProfit())
                            .direction(-1 * signal.getDirection()) //противоположно основному сигналу
                            .lot(getInteger(signal, client, signal.getTickerCode()))
                        .build());
            }
            publishSignalsService.publishPosition(signal);
        }
    }

}
