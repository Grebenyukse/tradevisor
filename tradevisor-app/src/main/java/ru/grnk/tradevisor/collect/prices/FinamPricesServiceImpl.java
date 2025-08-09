package ru.grnk.tradevisor.collect.prices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.common.repository.ParametersRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.integration.finam.FinamGrpcClientService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Service
@Slf4j
@ConditionalOnProperty(name = "app.collect.prices.finam", havingValue = "true")
public class FinamPricesServiceImpl {

    public static final String SHARES_TICKER_NAMES_LOADED = "shares_ticker_names_loaded";
    private final FinamGrpcClientService finamClient;
    private final TickersRepository tickersRepository;
    private final ParametersRepository parametersRepository;
    private final MarketDataRepository marketDataRepository;
    private final Map<String, PricesLoader> loaders;

    @Autowired

    public FinamPricesServiceImpl(FinamGrpcClientService finamClient,
                                  TickersRepository tickersRepository,
                                  ParametersRepository parametersRepository,
                                  MarketDataRepository marketDataRepository,
                                  List<PricesLoader> loaders) {
        this.finamClient = finamClient;
        this.tickersRepository = tickersRepository;
        this.parametersRepository = parametersRepository;
        this.marketDataRepository = marketDataRepository;
        this.loaders = loaders.stream().collect(toMap(PricesLoader::getProvider, x -> x));
    }

    @Scheduled
    public void doWork() {
        log.debug("start collecting prices");
        Map<String, String> parameters = parametersRepository.getAllParameters();
        if((!Boolean.parseBoolean(parameters.get(SHARES_TICKER_NAMES_LOADED)))) {
            log.debug("start collecting tickers");
            saveAllTickers();
            log.debug("tickers saved");
        }
        List<Tickers> tickers = tickersRepository.getAllTickers();
        tickers.stream().filter(x -> x.getProvider() != null)
                .forEach(x -> {
                    var loader = loaders.get(x.getProvider());
                    loader.loadPrices(x.getUuid());
                });
        log.debug("historic candles loaded");
    }

    @Transactional
    public void saveAllTickers() {
        finamClient.initTickers();
        parametersRepository.setValue(SHARES_TICKER_NAMES_LOADED, "true");
    }

    public void loadHistoryForTicker(String instrumentUUid) {
        var lastTimestamp = marketDataRepository.getLatestTickTime(instrumentUUid).toInstant();
        if (lastTimestamp.isAfter(Instant.now())) {
            return;
        }
    }
}