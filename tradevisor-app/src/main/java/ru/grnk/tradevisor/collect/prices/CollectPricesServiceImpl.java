package ru.grnk.tradevisor.collect.prices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.grnk.tradevisor.common.repository.ParametersRepository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.Share;
import ru.tinkoff.piapi.core.InvestApi;

import java.time.Instant;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor
public class CollectPricesServiceImpl {

    public static final String SHARES_TICKER_NAMES_LOADED = "shares_ticker_names_loaded";
    private final InvestApi investApi;
    private final TickersRepository tickersRepository;
    private final ParametersRepository parametersRepository;
    private final MarketDataRepository marketDataRepository;


    @Scheduled(cron = "${app.collect.prices.cron}")
    public void doWork() {
        log.debug("start collecting prices");
        Map<String, String> parameters = parametersRepository.getAllParameters();
        if (!Boolean.parseBoolean(parameters.get(SHARES_TICKER_NAMES_LOADED))) {
            log.debug("start collecting tickers");
            saveAllShares();
            log.debug("tickers saved");
        }
        List<Tickers> tickers = tickersRepository.getAllTickers();
        tickers.stream().map(Tickers::getUuid).forEach(this::loadHistoryForTicker);
        log.debug("historic candles loaded");
    }

    @Transactional
    public void saveAllShares() {
        var shares = investApi.getInstrumentsService().getAllSharesSync();
        shares.stream()
                .filter(Share::getShortEnabledFlag)
                .filter(Share::getApiTradeAvailableFlag)
                .filter(Share::getBuyAvailableFlag)
                .filter(s -> !s.getForQualInvestorFlag())
                .map(Shares2TickerMapper::from)
                .forEach(tickersRepository::saveInstrument);
        parametersRepository.setValue(SHARES_TICKER_NAMES_LOADED, "true");
    }

    public void loadHistoryForTicker(String instrumentUuid) {
        var lastTimestamp = marketDataRepository.getLatestTickTime(instrumentUuid).toInstant();
        if (lastTimestamp.isAfter(Instant.now())) {
            return;
        }
        investApi.getMarketDataService()
                .getCandlesSync(instrumentUuid, lastTimestamp, Instant.now(), CandleInterval.CANDLE_INTERVAL_1_MIN)
                .stream()
                .filter(HistoricCandle::getIsComplete)
                .forEach(c -> marketDataRepository.saveMarketData(c, instrumentUuid));
    }

}
