package ru.grnk.tradevisor.calculate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.calculate.strategies.IStrategy;
import ru.grnk.tradevisor.calculate.strategies.dto.TradingDirection;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.common.repository.SignalsRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CalculateSignalServiceImpl {

    private final TickersRepository tickersRepository;
    private final MarketDataRepository marketDataRepository;
    private final SignalsRepository signalsRepository;
    private final List<IStrategy> strategies;

    @Value("${app.calculate.batch-size:1000}")
    private int batchSize;

    @Scheduled(cron = "${app.calculate.cron}")
    public void doWork() {
        log.info("calculate all signals mf");

        int totalTickersCount = tickersRepository.getUnpublishedTickersCount();
        if (totalTickersCount == 0) {
            log.info("нет тикеров ждем когда появятся");
            return;
        }
        try (ProgressBar pb = new ProgressBarBuilder()
                .setTaskName("Calculate strategies")
                .setInitialMax(totalTickersCount)
                .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                .build()) {
            int offset = 0;
            List<Tickers> tickersBatch;
            do {
                tickersBatch = tickersRepository.getUnpublishedTickersBatch(batchSize, offset);
                if (tickersBatch.isEmpty()) {
                    break;
                }
                processTickersBatch(tickersBatch, pb);
                offset += batchSize;
            } while (tickersBatch.size() == batchSize);
        }
        log.info("all signals calculated");
    }

    private void processTickersBatch(List<Tickers> tickers, ProgressBar progressBar) {
        for (Tickers t : tickers) {
            var lastTickTime = marketDataRepository.getLatestTickTime(t.getTickerCode());
            strategies.forEach(s -> {
                var candles = marketDataRepository.fetchMarketDataForLast(s.barsRequiredToCalcStrategy(), t.getTickerCode());
                if (candles.size() < s.barsRequiredToCalcStrategy()) return;
                TrvCalculationResult result = s.calculate(candles);

                if (result.direction() != TradingDirection.UNKNOWN) {
                    signalsRepository.saveSignal(result, t.getTickerCode(), s.getStrategyUniqueName(), lastTickTime);
                }
            });
            progressBar.step();
            progressBar.setExtraMessage(t.getTickerCode());
        }
    }
}
