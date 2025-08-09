package ru.grnk.tradevisor.integration.tinkoff;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jvnet.hk2.annotations.Service;
import ru.grnk.tradevisor.collect.prices.PricesLoader;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.core.InvestApi;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TinkoffPricesService implements PricesLoader {

    private final InvestApi investApi;
    private final MarketDataRepository marketDataRepository;

    @Override
    public void loadPrices(String tickerUid) {
        loadHistoryForTicker(tickerUid);
    }

    @Override
    public String getProvider() {
        return "tinkoff";
    }

    public void loadHistoryForTicker(String instrumentUuid) {
        var lastTimestamp = marketDataRepository.getLatestTickTime(instrumentUuid).toInstant();
        if (lastTimestamp.isAfter(Instant.now())) {
            return;
        }
        investApi.getMarketDataService()
                .getCandlesSync(instrumentUuid, lastTimestamp, Instant.now(), CandleInterval.CANDLE_INTERVAL_5_MIN)
                .stream()
                .filter(HistoricCandle::getIsComplete)
                .forEach(c -> marketDataRepository.saveMarketData(c, instrumentUuid));
    }
}
