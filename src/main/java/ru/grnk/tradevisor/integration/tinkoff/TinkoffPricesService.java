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
//        торговать на тиньке очень дорого из-за высоких коммиссий. используем удобное API для биндинга spot-futures.
//        загружаем только те споты, по которым есть фьючи. фьючи будут загружаться под провайдером - финам.
        bindTradeFuturesService.initTickers();
    }

    @Override
    public void loadPrices(Tickers ticker) {
        // 1. Спот-актив (не фьючерс) -> spot_ticker_code == null
        //    ---> это бесконечный фьючерс?
        //         --> YES - не загружаем (он получает данные от фьючерсов)
        //         --> NO - загружаем котировки
        //
        // 2. Фьючерс -> spot_ticker_code != null
        //    ---> спот это бесконечный фьючерс?
        //         --> YES - это ближайший фьючерс по этому активу?
        //               --> YES - загрузи котировки и сохрани под тикером спота (бесконечного фьюча)
        //               --> NO - не загружаем
        //         --> NO - спот самостоятельный актив, не загружаем котировки по фьючу

        if (ticker.getSpotTickerCode() == null) {
            // Это спот-актив (возможно фьючерс)
            if (!isEndlessFutureTicker(ticker)) {
                // Это обычный спот-актив, загружаем котировки
                loadHistoryForTicker(ticker.getTickerCode(), tradevisorProperties.integration().tinkoff().historyMaxDepthDays());
            }
            // Если это бесконечный фьючерс, то его котировки будут приходить от обычных фьючерсов
        } else {
            // Это фьючерс
            if (isEndlessFuturesByTickerCode(ticker.getSpotTickerCode())) {
                // Спот - это бесконечный фьючерс, загружаем данные для него
                if (isNearestFutureCode(ticker.getTicker())) {
                    // Это ближайший фьючерс, загружаем его котировки для бесконечного фьючерса
                    loadHistoryForFuture(ticker.getSpotTickerCode(), ticker.getTickerCode(), tradevisorProperties.integration().tinkoff().historyMaxDepthDays());
                }
            }
            // Если спот - самостоятельный актив, то его котировки загружаются отдельно, фьючерс не трогаем
        }
    }

    private boolean isEndlessFuturesByTickerCode(String tickerCode) {
        return tickerCode != null && tickerCode.length() >= ENDLESS_FUTURES_CODE && tickerCode.endsWith("!1");
    }

    private boolean isEndlessFutureTicker(Tickers ticker) {
        return ticker.getTicker() != null && ticker.getTicker().endsWith("!1");
    }

    @Override
    public String getProvider() {
        return TRV_PROVIDER_TINKOFF;
    }

    @Override
    public int loadOrder() {
        return 3;
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
                .description(share.getName())
                .exchange(share.getExchange())
                .currency(share.getCurrency())
                .provider(TRV_PROVIDER_TINKOFF)
                .build();
    }

    private static Tickers from(Currency currency) {
        return Tickers.builder()
                .tickerCode(currency.getUid())
                .ticker(currency.getTicker())
                .description(currency.getName())
                .exchange(currency.getExchange())
                .currency(currency.getCurrency())
                .provider(TRV_PROVIDER_TINKOFF)
                .build();
    }

}
