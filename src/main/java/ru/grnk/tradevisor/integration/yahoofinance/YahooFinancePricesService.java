package ru.grnk.tradevisor.integration.yahoofinance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.collect.prices.PricesLoader;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.entity.MarketData;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.grnk.tradevisor.integration.yahoofinance.dto.YahooCandle;
import ru.grnk.tradevisor.integration.yahoofinance.dto.YahooTickerInfo;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.util.stream.Collectors.toList;
import static ru.grnk.tradevisor.common.util.TimeUtils.convertToTimestamp;


@Slf4j
@RequiredArgsConstructor
@Service
@ConditionalOnProperty(value = "app.collect.prices.yahoofinance")
public class YahooFinancePricesService implements PricesLoader {

    private final YahooFinanceService yahooFinanceService;
    private final MarketDataRepository marketDataRepository;
    private final TickersRepository tickersRepository;
    private final TradevisorProperties tradevisorProperties;

    @Override
    public void initTickers() {
        if (tickersRepository.getProviderTickersCount("yahoofinance") > 0) return;
        log.info("Start loading tickers for Yahoo Finance");
        var tickers = yahooFinanceService.fetchAllTickersFromJson();
        for (YahooTickerInfo symbol : tickers) {
            try {
                Tickers ticker = createTicker(symbol);
                tickersRepository.saveInstrument(ticker, "yahoofinance");
            } catch (Exception e) {
                log.warn("Failed to initialize ticker: " + symbol, e);
            }
        }
    }

    @Override
    public void loadPrices(String tickerCode) {
        int historyMaxDepthDays = tradevisorProperties.integration().yahoofinance().historyMaxDepthDays();
        var startTime = findStartTime(tickerCode, historyMaxDepthDays);
        var endTime = convertToTimestamp(ZonedDateTime.now());
        var intervalInHours = (endTime.getSeconds() - startTime.getSeconds()) / 3600;
        if (intervalInHours < 1) {
            return;
        }
        long from = startTime.getSeconds();
        var candles = yahooFinanceService.fetchHistoricalData(tickerCode, from);
        var marketDataList = candles.stream()
                .map(candle -> from(candle, tickerCode))
                .collect(toList());
        if (!marketDataList.isEmpty()) {
            marketDataRepository.batchInsertMarketData(marketDataList);
        }
    }

    @Override
    public String getProvider() {
        return "yahoofinance";
    }

    private com.google.protobuf.Timestamp findStartTime(String tickerCode, int historyMaxDepthDays) {
        var latestTime = marketDataRepository.getLatestTickTime(tickerCode, historyMaxDepthDays);
        return convertToTimestamp(latestTime);
    }

    private Tickers createTicker(YahooTickerInfo tickerInfo) {
        return Tickers.builder()
                .ticker(tickerInfo.symbol())
                .tickerCode(tickerInfo.symbol())
                .currency("USD")
                .exchange(tickerInfo.exchange())
                .expiration(null)
                .figi(tickerInfo.symbol())
                .description(tickerInfo.name())
                .precision(2)
                .marketType(tickerInfo.type())
                .lot(1)
                .provider("yahoofinance")
                .build();
    }

    private MarketData from(YahooCandle candle, String tickerCode) {
        return MarketData.builder()
                .time(Instant.ofEpochMilli(candle.timestamp()).atZone(ZoneId.of("UTC")).toOffsetDateTime())
                .open(candle.open())
                .high(candle.high())
                .low(candle.low())
                .close(candle.close())
                .tickerCode(tickerCode)
                .build();
    }

}
