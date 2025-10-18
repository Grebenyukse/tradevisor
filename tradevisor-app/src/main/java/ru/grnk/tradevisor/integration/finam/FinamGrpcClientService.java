package ru.grnk.tradevisor.integration.finam;

import com.google.protobuf.Timestamp;
import com.google.type.Interval;
import grpc.tradeapi.v1.assets.AssetsRequest;
import grpc.tradeapi.v1.assets.AssetsServiceGrpc;
import grpc.tradeapi.v1.assets.ExchangesRequest;
import grpc.tradeapi.v1.auth.AuthRequest;
import grpc.tradeapi.v1.auth.AuthServiceGrpc;
import grpc.tradeapi.v1.marketdata.BarsRequest;
import grpc.tradeapi.v1.marketdata.MarketDataServiceGrpc;
import grpc.tradeapi.v1.marketdata.TimeFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.collect.prices.PricesLoader;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.properties.TrvFinamProperties;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.integration.finam.repository.FinamMetainfoRepository;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "app.collect.prices.finam")
public class FinamGrpcClientService implements PricesLoader {

    private final TradevisorProperties properties;
    private final AssetsServiceGrpc.AssetsServiceBlockingStub assetsServiceBlockingStub;
    private final AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub;
    private final MarketDataServiceGrpc.MarketDataServiceBlockingStub marketDataServiceBlockingStub;
    private  final FinamMetainfoRepository finamMetainfoRepository;
    private final MarketDataRepository marketDataRepository;
    private final TickersRepository tickersRepository;

    public void initTickers() {
        initExchanges();
        var assetsRs = assetsServiceBlockingStub.withCallCredentials(getBearer())
                .assets(AssetsRequest.newBuilder().build());
        assetsRs.getAssetsList().forEach(finamMetainfoRepository::saveFinamAsset);
    }

    private void initExchanges() {
        var bearer = getBearer();
        var exchangesRs = assetsServiceBlockingStub.withCallCredentials(bearer)
                .exchanges(ExchangesRequest.newBuilder().build());
        exchangesRs.getExchangesList().forEach(finamMetainfoRepository::saveFinamExchange);
    }

    private BearerToken getBearer() {
        TrvFinamProperties finamProperties = properties.integration().finam();
        var authRs = authServiceBlockingStub.auth(AuthRequest.newBuilder()
                .setSecret(finamProperties.secret())
                .build());
        return new BearerToken(authRs.getToken());
    }

    public void loadHistoryForSymbol(String tickerCode) {
        var ticker = tickersRepository.findTickerByTickerCode(tickerCode);
        var symbol = tickerCode;
        log.debug("load prices for {}", symbol);
        var bearer = getBearer();
        var startTime = findStartTime(symbol);
        var endTime = convertToTimestamp(ZonedDateTime.now());
        if ((endTime.getSeconds() - startTime.getSeconds())/60 < 5) {
            return;
        }
        var marketDataRs = marketDataServiceBlockingStub
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
    }

    private Timestamp convertToTimestamp(ZonedDateTime zonedDateTime) {
        var instant = zonedDateTime.toInstant();
        return com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    private Timestamp convertToTimestamp(OffsetDateTime offsetDateTime) {
        var instant = offsetDateTime.toInstant();
        return com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    private Timestamp  findStartTime(String symbol) {
        var res = marketDataRepository.getLatestTickTime(symbol);
        return convertToTimestamp(res);
    }

    @Override
    public void loadPrices(String tickerUid) {
        loadHistoryForSymbol(tickerUid);
    }

    @Override
    public String getProvider() {
        return "finam";
    }
}
