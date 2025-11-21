package ru.grnk.tradevisor.integration.yahoofinance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.collect.prices.PricesLoader;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;
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

    private final YahooFinanceClient yahooFinanceClient;
    private final MarketDataRepository marketDataRepository;
    private final TickersRepository tickersRepository;
    private final TradevisorProperties tradevisorProperties;

    @Override
    public void initTickers() {
        if (tickersRepository.getProviderTickersCount("yahoofinance") > 0) return;
        log.info("Start loading tickers for Yahoo Finance");
        var tickers = yahooFinanceClient.fetchAllTickers();
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
        long to = endTime.getSeconds();
        var candles = yahooFinanceClient.fetchHistoricalData(tickerCode, from, to);
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
        return new Tickers()
                .setTicker(tickerInfo.symbol())
                .setTickerCode(tickerInfo.symbol())
                .setCurrency("USD")
                .setExchange(tickerInfo.exchange())
                .setExpiration(null)
                .setFigi(tickerInfo.symbol())
                .setDescription(tickerInfo.name())
                .setPrecision(2)
                .setMarketType(tickerInfo.type())
                .setLot(1)
                .setProvider("yahoofinance");
    }

    private MarketData from(YahooCandle candle, String tickerCode) {
        return new MarketData()
                .setTime(Instant.ofEpochMilli(candle.timestamp()).atZone(ZoneId.of("UTC")).toOffsetDateTime())
                .setOpen(candle.open())
                .setHigh(candle.high())
                .setLow(candle.low())
                .setClose(candle.close())
                .setTickerCode(tickerCode);
    }

}
