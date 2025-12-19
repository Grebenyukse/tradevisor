package ru.grnk.tradevisor.integration.tinkoff;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.collect.prices.PricesLoader;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.contract.v1.Currency;
import ru.tinkoff.piapi.core.InvestApi;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.collect.prices.tinkoff")
public class TinkoffPricesService implements PricesLoader {

    public static final String TINKOFF_PROVIDER_NAME = "tinkoff";
    private final InvestApi investApi;
    private final MarketDataRepository marketDataRepository;
    private final TickersRepository tickersRepository;
    private final TradevisorProperties tradevisorProperties;

    @SneakyThrows
    @Override
    public void initTickers() {
        if (tickersRepository.getProviderTickersCount(this.getProvider()) > 0) return;
        investApi.getInstrumentsService().getAllShares().get(10, TimeUnit.SECONDS).stream()
                .map(TinkoffPricesService::from)
                .forEach(tickersRepository::saveInstrument);
        investApi.getInstrumentsService().getAllCurrencies().get(10, TimeUnit.SECONDS).stream()
                .map(TinkoffPricesService::from)
                .forEach(tickersRepository::saveInstrument);
    }

    @Override
    public void loadPrices(String tickerUid) {
        loadHistoryForTicker(tickerUid, tradevisorProperties.integration().tinkoff().historyMaxDepthDays());
    }

    @Override
    public String getProvider() {
        return TINKOFF_PROVIDER_NAME;
    }

    public void loadHistoryForTicker(String instrumentUuid, int historyMaxDepthDays) {
        var lastTimestamp = marketDataRepository.getLatestTickTime(instrumentUuid, historyMaxDepthDays).toInstant();
        if (lastTimestamp.isAfter(Instant.now())) {
            return;
        }
        investApi.getMarketDataService()
                .getCandlesSync(instrumentUuid, lastTimestamp, Instant.now(), CandleInterval.CANDLE_INTERVAL_HOUR)
                .stream()
                .filter(HistoricCandle::getIsComplete)
                .forEach(c -> marketDataRepository.saveMarketData(c, instrumentUuid));
    }

    private static Tickers from(Share share) {
        return Tickers.builder()
                .tickerCode(share.getUid())
                .ticker(share.getTicker())
                .figi(share.getFigi())
                .description(share.getName())
                .exchange(share.getExchange())
                .marketType("shares")
                .currency(share.getCurrency())
                .expiration(null)
                .go(null)
                .lot(share.getLot())
                .precision(1)
                .provider(TINKOFF_PROVIDER_NAME)
                .build();
    }

    private static Tickers from(Currency currency) {
        return Tickers.builder()
                .tickerCode(currency.getUid())
                .ticker(currency.getTicker())
                .figi(currency.getFigi())
                .description(currency.getName())
                .exchange(currency.getExchange())
                .marketType("currencies")
                .currency(currency.getCurrency())
                .expiration(null)
                .go(null)
                .lot(currency.getLot())
                .precision(1)
                .provider(TINKOFF_PROVIDER_NAME)
                .build();
    }
}
