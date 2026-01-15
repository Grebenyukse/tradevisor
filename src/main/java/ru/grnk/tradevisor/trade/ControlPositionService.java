package ru.grnk.tradevisor.trade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.calculate.signals.TrvSignalStatus;
import ru.grnk.tradevisor.calculate.strategies.IStrategy;
import ru.grnk.tradevisor.collect.prices.TelegramNotificationService;
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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static ru.grnk.tradevisor.calculate.signals.TrvSignalStatus.*;

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
    private final PublishSignalsService publishSignalsService;
    private final TelegramNotificationService telegramNotificationService;

    @Scheduled(fixedRateString = "${app.trade.delay}")
    public void process() {
        List<Signals> allRelevantSignals = signalsRepository.findSignalsByStatuses(
                List.of(
                        CREATED.name(),
                        PUBLISHED.name(),
                        CONFIRMED.name(),
                        EXECUTED.name()
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
        try {
            result.active.forEach(
                    s -> {
                        switch (TrvSignalStatus.valueOf(s.getStatus())) {
                            case CREATED:
                            case PUBLISHED:
                                log.debug("решение по сигналу {} не принято", s.getId());
                                return;
                            case CONFIRMED:
                                this.openPosition(s);
                                return;
                            case EXECUTED:
                                this.controlPosition(s);
                        }
                    }
            );
        } catch (Exception e) {
            log.error("ошибка открытия или контроля позиции", e);
            telegramNotificationService.sendControlPositionErrorMessage(e);
        }

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
        Tickers spotTicker = tickersRepository.getTickerByTickerCode(signal.getTickerCode());
        Tickers ticker = tickersRepository.findTradeTickerByTickerCodeIfExists(spotTicker.getTickerCode()).orElse(spotTicker);
        var clientOptional = tradeClients.stream().filter(tc -> Objects.equals(tc.provider(), ticker.getProvider()))
                .findFirst();
        if (clientOptional.isEmpty()) {
            log.warn("провайдер {} для сигнала signal:{} по spot_ticker_code: {} не активен. невозможно выполнить торговую операцию.",
                   ticker.getProvider(), signal, ticker.getTickerCode());
            return;
        }
        var client = clientOptional.get();
        // проверяем сохранились ли предусловия для открытия позиции
        var strategy = strategies.stream().filter(s -> Objects.equals(s.getStrategyUniqueName(), signal.getName())).findFirst().orElseThrow();
        var candles = marketDataRepository.fetchMarketDataForLast(strategy.barsRequiredToCalcStrategy(), signal.getTickerCode());
        var strategyCalculationResult = strategy.calculate(candles);
        if (strategyCalculationResult.direction().directionCode() != signal.getDirection()) {
            log.info("отменен сигнал {} по причине нарушения базовых условий стратегии {}", signal.getId(), strategy.getStrategyUniqueName());
            signalsRepository.updateSignalStatus(signal.getId(), TrvSignalStatus.CANCELLED);
            return;
        }
        // проверяем есть ли открытые ордера по тикеру
        List<TrvOrder> orders = client.getOrdersByTicker(ticker.getTickerCode());
        if (!orders.isEmpty()) {
            log.error("сигнал {} находится в статусе {}, но по нему есть открытые ордера {}", signal, signal.getStatus(), orders.size());
            signalsRepository.updateSignalStatus(signal.getId(), TrvSignalStatus.MANUAL);
            publishSignalsService.publishOrderForManualExecution(signal);
            return;
        }
        // проверяем есть ли открытые позиции по тикеру
        if (client.isPositionOpened(ticker.getTickerCode())) {
            log.error("сигнал {} находится в статусе {}, но по нему есть открытая позиция.", signal, signal.getStatus());
            signalsRepository.updateSignalStatus(signal.getId(), TrvSignalStatus.MANUAL);
            publishSignalsService.publishOrderForManualExecution(signal);
            return;
        }
        // сигнал жив, ордеров нет, позиций нет, сигнал подтвержден пользователем -> выставляем ордера
        if (client.openPosition(signal)) {
            signalsRepository.updateSignalStatus(signal.getId(), TrvSignalStatus.EXECUTED);
            publishSignalsService.publishOrder(signal);
        } else {
            log.warn("Не удалось автоматически открыть позицию. Переводим сигнал в режим ручного управления. Signal: {}", signal);
            signalsRepository.updateSignalStatus(signal.getId(), TrvSignalStatus.MANUAL);
            publishSignalsService.publishOrderForManualExecution(signal);
        }
    }

    public void controlPosition(Signals signal) {
        Tickers spotTicker = tickersRepository.getTickerByTickerCode(signal.getTickerCode());
        Tickers ticker = tickersRepository.findTradeTickerByTickerCodeIfExists(spotTicker.getTickerCode()).orElse(spotTicker);
        var client = tradeClients.stream().filter(tc -> Objects.equals(tc.provider(), ticker.getProvider()))
                .findFirst()
                .orElseThrow();
        if (client.isPositionOpened(ticker.getTickerCode())) {
            try {
                client.checkPositionStatus(ticker.getTickerCode());
            } catch (Exception e) {
                log.warn("ошибка валидации позиции", e);
                signalsRepository.updateSignalStatus(signal.getId(), TrvSignalStatus.MANUAL);
            }
        } else {
            var strategy = strategies.stream()
                    .filter(s -> Objects.equals(s.getStrategyUniqueName(), signal.getName()))
                    .findFirst()
                    .orElseThrow();
            var candles = marketDataRepository.fetchMarketDataForLast(strategy.barsRequiredToCalcStrategy(), signal.getTickerCode());
            var strategyCalculationResult = strategy.calculate(candles);
            if (strategyCalculationResult.direction().directionCode() != signal.getDirection()) {
                log.info("предпосылки торгового сигнала нарушены. удаляем ордера. сигнал переводим в статус cancelled  Signal: {}", signal);
                client.deleteOrders(ticker.getTickerCode());
                signalsRepository.updateSignalStatus(signal.getId(), TrvSignalStatus.CANCELLED);
            }
        }
    }

}
