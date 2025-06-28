package ru.grnk.tradevisor.calculate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.calculate.strategies.IStrategy;
import ru.grnk.tradevisor.calculate.strategies.TradingDirection;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.common.repository.SignalsRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CalculateSignalServiceImpl implements CalculateSignalService {

    private final TickersRepository tickersRepository;
    private final MarketDataRepository marketDataRepository;
    private final SignalsRepository signalsRepository;
    private final List<IStrategy> strategies;

    @Override
    @Scheduled(cron = "${app.calculate.cron}")
    public void doWork() {
        log.info("calculate all signals mf");
        var tickers = tickersRepository.getAllTickers();
        if (tickers.isEmpty()) {
            log.info("нет тикеров ждем когда появятся");
            return;
        }
        for (Tickers t : tickers) {
            var lastTickTime = marketDataRepository.getLatestTickTime(t.getUuid());
            strategies.forEach(s -> {
                var candles = marketDataRepository.fetchMarketDataForLast(s.historyDepthBarsBy1m(), t.getUuid());
                var result = s.calculate(candles);
                if (result.direction() != TradingDirection.UNKNOWN) {
                    signalsRepository.saveSignal(result, t.getUuid(), s.getStrategyUniqueName(), lastTickTime);
                }
            });
        }
    }

}
