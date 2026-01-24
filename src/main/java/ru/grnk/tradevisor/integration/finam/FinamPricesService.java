package ru.grnk.tradevisor.integration.finam;

import com.google.protobuf.Timestamp;
import com.google.type.Decimal;
import com.google.type.Interval;
import grpc.tradeapi.v1.auth.AuthRequest;
import grpc.tradeapi.v1.auth.AuthServiceGrpc;
import grpc.tradeapi.v1.marketdata.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.collect.prices.PricesLoader;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.properties.TrvFinamProperties;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.entity.Tickers;

import java.time.ZonedDateTime;

import static java.util.Optional.ofNullable;
import static ru.grnk.tradevisor.common.util.TimeUtils.convertToTimestamp;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "app.collect.prices.finam")
public class FinamPricesService implements PricesLoader {

    public static final int MIN_TICKER_ALIVE_TIME_INTERVAL_TO_KICK = 720;
    public static final String TRV_PROVIDER_FINAM = "finam";
    private final TradevisorProperties properties;
    private final AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub;
    private final MarketDataServiceGrpc.MarketDataServiceBlockingStub marketDataServiceBlockingStub;

    private final MarketDataRepository marketDataRepository;
    private final TickersRepository tickersRepository;

    public void initTickers() {
        log.debug("skip");
    }

    public void loadHistoryForSymbol(String tickerCode) {
        var symbol = tickerCode;
        log.debug("load prices for {}", symbol);
        var bearer = getBearer();
        var startTime = findStartTime(symbol, properties.integration().finam().historyMaxDepthDays());
        var endTime = convertToTimestamp(ZonedDateTime.now());
        var intervalInHours = (endTime.getSeconds() - startTime.getSeconds()) / 60;
        if (intervalInHours < 5) {
            return;
        }
        if (!symbol.contains("@")) {
            log.warn("no mic in symbol detected: {}", symbol);
        }
        BarsResponse marketDataRs;
        marketDataRs = marketDataServiceBlockingStub
                .withCallCredentials(bearer)
                .bars(BarsRequest.newBuilder()
                        .setInterval(Interval.newBuilder()
                                .setStartTime(startTime)
                                .setEndTime(endTime)
                                .build())
                        .setSymbol(symbol)
                        .setTimeframe(TimeFrame.TIME_FRAME_H1)
                        .build());
        marketDataRs.getBarsList().stream().forEach(b -> marketDataRepository.saveMarketData(b, tickerCode));
        if (marketDataRs.getBarsList().isEmpty() && intervalInHours > MIN_TICKER_ALIVE_TIME_INTERVAL_TO_KICK) {
            tickersRepository.markTickerFailedByQuotes(tickerCode);
        }
    }

    private BearerToken getBearer() {
        TrvFinamProperties finamProperties = properties.integration().finam();
        var authRs = authServiceBlockingStub.auth(AuthRequest.newBuilder()
                .setSecret(finamProperties.secret())
                .build());
        return new BearerToken(authRs.getToken());
    }

    private Timestamp findStartTime(String symbol, int historyMaxDepthDays) {
        var res = marketDataRepository.getLatestTickTime(symbol, historyMaxDepthDays);
        return convertToTimestamp(res);
    }

    @Override
    public void loadPrices(Tickers ticker) {
        loadHistoryForSymbol(ticker.getTickerCode());
    }

    @Override
    public String getProvider() {
        return TRV_PROVIDER_FINAM;
    }

    @Override
    public int loadOrder() {
        return 4;
    }

    @Override
    public float getBidForTicker(String tickerCode) {
        int maxRetries = 10;
        long delayMillis = 10_000; // 10 секунд
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("getBidForTicker before. Attempt {}/{}. Ticker: {}", attempt, maxRetries, tickerCode);
                var res = marketDataServiceBlockingStub
                        .withCallCredentials(getBearer())
                        .lastQuote(QuoteRequest.newBuilder()
                                .setSymbol(tickerCode)
                                .build());
                log.info("getBidForTicker after. Attempt {}/{}. Ticker: {}", attempt, maxRetries, tickerCode);
                return ofNullable(res)
                        .map(QuoteResponse::getQuote)
                        .map(Quote::getBid)
                        .map(Decimal::getValue)
                        .map(Float::parseFloat)
                        .orElseThrow(() -> new RuntimeException("No value present for bid of ticker: " + tickerCode));

            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed for ticker {}: {}", attempt, maxRetries, tickerCode, e.getMessage(), e);
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(delayMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while waiting to retry", ie);
                    }
                }
            }
        }
        log.error("All {} attempts failed for ticker {}", maxRetries, tickerCode, lastException);
        throw new RuntimeException("Failed to get bid for ticker after " + maxRetries + " attempts.", lastException);
    }
}
