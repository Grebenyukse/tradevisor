package ru.grnk.tradevisor.collect.prices;

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

    @Scheduled(cron = "${app.collect.prices.cron}")
    public void doWork() {
        log.debug("start collecting prices");
        Map<String, String> parameters = parametersRepository.getAllParameters();
        if(!Boolean.parseBoolean(parameters.get(SHARES_TICKER_NAMES_LOADED))) {
            log.debug("init tickers");
            loaders.values().forEach(PricesLoader::initTickers);
            log.debug("tickers saved");
        }
        List<Tickers> tickers = tickersRepository.getAllTickers();
        tickers.stream().filter(x -> x.getProvider() != null)
                .forEach(x -> {
                    var loader = loaders.get(x.getProvider());
                    loader.loadPrices(x.getTickerCode());
                });
        log.debug("historic candles loaded");
    }
}