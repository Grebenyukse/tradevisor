package ru.grnk.tradevisor.integration.bybit;

import com.bybit.api.client.domain.CategoryType;
import com.bybit.api.client.domain.asset.request.AssetDataRequest;
import com.bybit.api.client.domain.market.MarketInterval;
import com.bybit.api.client.domain.market.request.MarketDataRequest;
import com.bybit.api.client.restApi.BybitApiAssetRestClient;
import com.bybit.api.client.restApi.BybitApiMarketRestClient;
import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.collect.prices.PricesLoader;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.entity.MarketData;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.grnk.tradevisor.common.util.ObjectMapperUtils;
import ru.grnk.tradevisor.integration.bybit.dto.BybitCandlesResponse;

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
    private final BybitApiMarketRestClient marketRestClient;
    private final BybitApiAssetRestClient assetRestClient;

    @SneakyThrows
    @Override
    public void loadPrices(Tickers ticker) {
        var tickerCode = ticker.getTickerCode();
        checkTickerExists(tickerCode);
        var startTime = findStartTime(tickerCode);
        var endTime = convertToTimestamp(ZonedDateTime.now());
        var intervalInHours = (endTime.getSeconds() - startTime.getSeconds()) / 60;
        if (intervalInHours < 5) {
            return;
        }
        var response = marketRestClient.getMarketLinesData(MarketDataRequest.builder()
                .symbol(ticker.getTicker())
                .category(CategoryType.LINEAR)
                .startTime(startTime.getSeconds())
                .endTime(endTime.getSeconds())
                .marketInterval(MarketInterval.FOUR_HOURLY)
                .limit(500)
                .build());
        var parsedResponse = ObjectMapperUtils.readValue(ObjectMapperUtils.writeValue(response), BybitCandlesResponse.class);

        var res = parsedResponse.result().list().stream()
                .map(x -> from(x, tickerCode))
                .collect(toList());
        marketDataRepository.batchInsertMarketData(res);
    }

    private boolean checkTickerExists(String tickerCode) {
        try {
            assetRestClient.getAssetInfo(AssetDataRequest.builder()
                    .symbol(tickerCode.split("@")[0])
                    .build());
            return true;
        } catch (Exception e) {
            log.error("ошибка проверки наличия тикера: {}",tickerCode,  e);
            return false;
        }
    }

    @Override
    public String getProvider() {
        return "bybit";
    }

    @SneakyThrows
    @Override
    public void initTickers() {
        log.info("start loading tickers for bybit");
        var tickers = bybitClient.fetchAllTickers();
        tickers.stream()
                .map(BybitPricesService::from)
                .filter(x -> x.getTicker().endsWith("USDT")) // торгуем только прямые инструменты
                .forEach(tickersRepository::saveInstrument);
    }

    @Override
    public int loadOrder() {
        return 1;
    }

    private Timestamp findStartTime(String symbol) {
        var res = marketDataRepository.getLatestTickTime(symbol, tradevisorProperties.integration().bybit().historyMaxDepthDays());
        return convertToTimestamp(res);
    }

    private static Tickers from(BybitTickerRs.SymbolInfo bybitTicker) {
        return Tickers.builder()
                .ticker(bybitTicker.symbol())
                .tickerCode(bybitTicker.symbol() + "@" + "bybit")
                .currency(bybitTicker.quoteCoin())
                .exchange("bybit")
                .description(bybitTicker.status())
                .provider("bybit")
                .build();
    }

    private static MarketData from(BybitCandlesResponse.Candle candlestick, String tickerCode) {
        return new MarketData(
                tickerCode,
                Instant.ofEpochMilli(candlestick.openTime()).atZone(ZoneId.of("Europe/Moscow")).toOffsetDateTime(),
                candlestick.openPrice().floatValue(),
                candlestick.highPrice().floatValue(),
                candlestick.lowPrice().floatValue(),
                candlestick.closePrice().floatValue()
        );
    }

    @Override
    public float getBidForTicker(String tickerCode) {
        throw new NotImplementedException();
    }

}
