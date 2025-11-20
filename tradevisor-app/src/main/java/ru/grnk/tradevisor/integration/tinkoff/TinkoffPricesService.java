package ru.grnk.tradevisor.integration.tinkoff;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.collect.prices.PricesLoader;
import ru.grnk.tradevisor.collect.utils.Shares2TickerMapper;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.core.InvestApi;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.collect.prices.tinkoff")
public class TinkoffPricesService implements PricesLoader {

    private final InvestApi investApi;
    private final MarketDataRepository marketDataRepository;
    private final TickersRepository tickersRepository;
    private final TradevisorProperties tradevisorProperties;

    @Override
    public void initTickers() {
        if (tickersRepository.getProviderTickersCount("tinkoff") > 0) return;
        var shares = investApi.getInstrumentsService().getAllShares();
        try {
            shares.get(10, TimeUnit.SECONDS).stream()
                    .map(Shares2TickerMapper::from)
                    .forEach(x -> tickersRepository.saveInstrument(x, "tinkoff"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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
                .getCandlesSync(instrumentUuid, lastTimestamp, Instant.now(), CandleInterval.CANDLE_INTERVAL_HOUR)
                .stream()
                .filter(HistoricCandle::getIsComplete)
                .forEach(c -> marketDataRepository.saveMarketData(c, instrumentUuid));
    }
}
