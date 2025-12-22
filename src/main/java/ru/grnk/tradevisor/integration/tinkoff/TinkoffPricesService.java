package ru.grnk.tradevisor.integration.tinkoff;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.collect.prices.BindTradeFuturesService;
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

import static ru.grnk.tradevisor.collect.prices.BindTradeFuturesService.TRV_PROVIDER_TINKOFF;
import static ru.grnk.tradevisor.integration.tinkoff.FutureUtils.isNearestFutureCode;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.collect.prices.tinkoff")
public class TinkoffPricesService implements PricesLoader {

    public static final String TRV_ASSET_TYPE_SHARES = "shares";
    public static final int ENDLESS_FUTURES_CODE = 4;
    private final InvestApi investApi;
    private final MarketDataRepository marketDataRepository;
    private final TickersRepository tickersRepository;
    private final TradevisorProperties tradevisorProperties;
    private final BindTradeFuturesService bindTradeFuturesService;

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
        bindTradeFuturesService.initTickers();
    }

    @Override
    public void loadPrices(Tickers ticker) {
// 1. спот      -> spot_ticker_code == null
//                    ---> это бесконечный фьючерс?
//                      --> YES - return
//                      --> NO - Load tickers
// 2. futures   -> spot_ticker_code != nul
//                      ---> спот это бесконечный фьючерс?  -> по коду тикера
//                      |      --> это ближайший фьючерс по этому активу?
//                      |         --> загрузи котировки и сохрани под тикером спота (бесконечного фьюча)
//                      |
//                      ---> спот - самостоятельный актив. пусть ищется по своим котировкам. не загружать котировки по фьючу.

        if (ticker.getSpotTickerCode() == null) {
            if (!isEndlessFutureTicker(ticker)) {
                loadHistoryForTicker(ticker.getTickerCode(), tradevisorProperties.integration().tinkoff().historyMaxDepthDays());
            }
        } else {
            if (isEndlessFuturesByTickerCode(ticker.getSpotTickerCode())) {
                if (isNearestFutureCode(ticker.getTicker())) {
                    loadHistoryForFuture(ticker.getSpotTickerCode(), ticker.getTickerCode(), tradevisorProperties.integration().tinkoff().historyMaxDepthDays());
                }
            }
        }
    }

    private boolean isEndlessFuturesByTickerCode(String tickerCode) {
        return tickerCode!= null && tickerCode.length() == ENDLESS_FUTURES_CODE && tickerCode.endsWith("!1");
    }

    private boolean isEndlessFutureTicker(Tickers ticker) {
        return ticker.getSpotTickerCode() != null && ticker.getSpotTickerCode().length() == TinkoffPricesService.ENDLESS_FUTURES_CODE
                && ticker.getTicker().contains("!1");
    }

    @Override
    public String getProvider() {
        return TRV_PROVIDER_TINKOFF;
    }

    @Override
    public int loadOrder() {
        return 100;
    }

    public void loadHistoryForFuture(String endlessFutureCode, String nearestFutureCode, int historyMaxDepthDays) {
        var lastTimestamp = marketDataRepository.getLatestTickTime(endlessFutureCode, historyMaxDepthDays).toInstant();
        if (lastTimestamp.isAfter(Instant.now())) {
            return;
        }
        investApi.getMarketDataService()
                .getCandlesSync(nearestFutureCode, lastTimestamp, Instant.now(), CandleInterval.CANDLE_INTERVAL_HOUR)
                .stream()
                .filter(HistoricCandle::getIsComplete)
                .forEach(c -> marketDataRepository.saveMarketData(c, endlessFutureCode));
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
                .marketType(TRV_ASSET_TYPE_SHARES)
                .currency(share.getCurrency())
                .expiration(null)
                .go(null)
                .lot(share.getLot())
                .precision(1)
                .provider(TRV_PROVIDER_TINKOFF)
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
                .provider(TRV_PROVIDER_TINKOFF)
                .build();
    }

}
