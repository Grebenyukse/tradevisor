package ru.grnk.tradevisor.integration.bybit;

import com.google.protobuf.Timestamp;
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.util.stream.Collectors.toList;
import static ru.grnk.tradevisor.common.util.TimeUtils.convertToTimestamp;

@Slf4j
@RequiredArgsConstructor
@Service
@ConditionalOnProperty(value = "app.collect.prices.bybit")
public class BybitPricesService implements PricesLoader {

    private final BybitClient bybitClient;
    private final MarketDataRepository marketDataRepository;
    private final TickersRepository tickersRepository;
    private final TradevisorProperties tradevisorProperties;


    @Override
    public void loadPrices(String tickerCode) {
        var startTime = findStartTime(tickerCode);
        var endTime = convertToTimestamp(ZonedDateTime.now());
        var intervalInHours = (endTime.getSeconds() - startTime.getSeconds())/60;
        if (intervalInHours < 5) {
            return;
        }
        var candles = bybitClient.fetchHourlyCandles(tickerCode, startTime.getSeconds(), 500);
        var res  = candles.stream()
                .map(x -> from(x, tickerCode))
                .collect(toList());
        marketDataRepository.batchInsertMarketData(res);
    }

    @Override
    public String getProvider() {
        return "bybit";
    }

    @Override
    public void initTickers() {
        if (tickersRepository.getProviderTickersCount("bybit") > 0) return;
        log.info("start loading tickers for bybit");
        var tickers = bybitClient.fetchAllTickers();
        tickers.stream()
                .map(BybitPricesService::from)
                .forEach(x -> tickersRepository.saveInstrument(x, "bybit"));
    }

    private Timestamp findStartTime(String symbol) {
        var res = marketDataRepository.getLatestTickTime(symbol);
        return convertToTimestamp(res);
    }

    private static Tickers from(BybitTickerRs.SymbolInfo bybitTicker) {
        return new Tickers()
                .setTicker(bybitTicker.symbol())
                .setTickerCode(bybitTicker.symbol() + "@" + "bybit")
                .setCurrency(bybitTicker.baseCoin())
                .setExchange("bybit")
                .setExpiration(null)
                .setFigi(bybitTicker.symbol() + "@" + "bybit")
                .setDescription(bybitTicker.status())
                .setPrecision(bybitTicker.lotSizeFilter().basePrecision().precision())
                .setLot(1)
                .setProvider("bybit");
    }

    private static MarketData from(BybitMarketdataRs.Candlestick candlestick, String tickerCode) {
        return new MarketData()
                .setTime(Instant.ofEpochMilli(candlestick.openTime()).atZone(ZoneId.of("Europe/Moscow")).toOffsetDateTime())
                .setOpen(candlestick.openPrice())
                .setHigh(candlestick.highPrice())
                .setLow(candlestick.lowPrice())
                .setClose(candlestick.closePrice())
                .setTickerCode(tickerCode);
    }

}
