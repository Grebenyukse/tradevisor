package ru.grnk.tradevisor.integration.finam;

import com.google.protobuf.Timestamp;
import com.google.type.Interval;
import grpc.tradeapi.v1.accounts.AccountsServiceGrpc;
import grpc.tradeapi.v1.assets.AssetsRequest;
import grpc.tradeapi.v1.assets.AssetsServiceGrpc;
import grpc.tradeapi.v1.assets.ExchangesRequest;
import grpc.tradeapi.v1.auth.AuthRequest;
import grpc.tradeapi.v1.auth.AuthServiceGrpc;
import grpc.tradeapi.v1.marketdata.BarsRequest;
import grpc.tradeapi.v1.marketdata.MarketDataServiceGrpc;
import grpc.tradeapi.v1.marketdata.TimeFrame;
import grpc.tradeapi.v1.orders.OrdersServiceGrpc;
import liquibase.pro.packaged.B;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.http.util.TimeStamp;
import org.jvnet.hk2.annotations.Service;
import ru.grnk.tradevisor.collect.prices.PricesLoader;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.properties.TrvFinamProperties;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.integration.finam.repository.FinamMetainfoRepository;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinamGrpcClientService implements PricesLoader {

    private final TradevisorProperties properties;
    private final AccountsServiceGrpc.AccountsServiceBlockingStub accountsServiceBlockingStub;
    private final AssetsServiceGrpc.AssetsServiceBlockingStub assetsServiceBlockingStub;
    private final AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub;
    private final MarketDataServiceGrpc.MarketDataServiceBlockingStub marketDataServiceBlockingStub;
    private final OrdersServiceGrpc.OrdersServiceBlockingStub ordersServiceBlockingStub;
    private  final FinamMetainfoRepository finamMetainfoRepository;
    private final TickersRepository tickersRepository;
    private final MarketDataRepository marketDataRepository;

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

    public void loadHistoryForSymbol(String symbol) {
        TrvFinamProperties finamProperties = properties.integration().finam();
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
                        .setTimeframe(TimeFrame.TIME_FRAME_M5)
                        .build());
        // persist marketData
        marketDataRs.getBarsList().stream().forEach(b -> {});
    }

    public void loadMarketData(List<String> symbols) {
        TrvFinamProperties finamProperties = properties.integration().finam();
        var bearer = getBearer();
        for (var symbol : symbols) {
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
                            .setTimeframe(TimeFrame.TIME_FRAME_M5)
                            .build());
            // persist marketData
            marketDataRs.getBarsList().stream().forEach(b -> {});
        }
    }

    private Timestamp convertToTimestamp(ZonedDateTime zonedDateTime) {
        var instant = zonedDateTime.toInstant();
        return com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    private Timestamp findStartTime(String symbol) {
        var res = marketDataRepository.getLatestTickTime(symbol);
        return res;
    }

    @Override
    public void loadPrices(String tickerUid) {
        String ticker = finamMetainfoRepository.findTickerByUid(tickerUid);
        loadHistoryForSymbol(ticker);
        log.info("load prices");
    }

    @Override
    public String getProvider() {
        return "finam";
    }
}
