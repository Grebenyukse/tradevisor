package ru.grnk.tradevisor.collect.prices;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.common.repository.ParametersRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Service
@Slf4j
public class PricesLoaderServiceImpl {

    public static final String SHARES_TICKER_NAMES_LOADED = "shares_ticker_names_loaded";
    private final TickersRepository tickersRepository;
    private final ParametersRepository parametersRepository;
    private final Map<String, PricesLoader> loaders;

    @Autowired
    public PricesLoaderServiceImpl(TickersRepository tickersRepository,
                                   ParametersRepository parametersRepository,
                                   List<PricesLoader> pricesLoaders) {
        this.tickersRepository = tickersRepository;
        this.parametersRepository = parametersRepository;
        this.loaders = pricesLoaders.stream().collect(toMap(PricesLoader::getProvider, x -> x));
    }

    @SneakyThrows
    @Scheduled(cron = "${app.collect.prices.cron}")
    public void doWork() {
        log.debug("start collecting prices");
        Map<String, String> parameters = parametersRepository.getAllParameters();
        if (!Boolean.parseBoolean(parameters.get(SHARES_TICKER_NAMES_LOADED))) {
            log.debug("init tickers");
            loaders.values().forEach(PricesLoader::initTickers);
            log.debug("tickers saved");
        }
        List<Tickers> tickers = tickersRepository.getAllTickers();
        try {
            tickers.stream().filter(x -> x.getProvider() != null)
                    .forEach(x -> {
                        try {
                            var loader = loaders.get(x.getProvider());
                            if (loader == null) return;
                            loader.loadPrices(x.getTickerCode());
                        } catch (Exception e) {
                            if (e.getMessage().contains("Security id doesn't exist for mic")) {
                                tickersRepository.markTickerFailed(x.getTickerCode());
                                log.warn("ticker {} excluded as unknown. will not be requested next time.", x.getTickerCode());
                                return;
                            }
                            if (e.getMessage().contains("Api token could not be verified")) {
                                log.error("unauthenticated when loading ticker: {}", x.getTickerCode());
                                return;
                            }
                            throw new RuntimeException(e);
                        }
                    });
        } catch (Exception e) {
            if (e.getMessage().contains("RESOURCE_EXHAUSTED")) {
                log.warn("resource exhausted");
                return;
            } else {
                log.error("failed to load market data", e);
            }
            throw new RuntimeException("oshibka", e);
        }

        log.debug("historic candles loaded");
    }
}