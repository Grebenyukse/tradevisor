package ru.grnk.tradevisor.integration.finam;

import com.google.protobuf.Timestamp;
import com.google.type.Interval;
import grpc.tradeapi.v1.assets.AssetsRequest;
import grpc.tradeapi.v1.assets.AssetsServiceGrpc;
import grpc.tradeapi.v1.assets.ExchangesRequest;
import grpc.tradeapi.v1.auth.AuthRequest;
import grpc.tradeapi.v1.auth.AuthServiceGrpc;
import grpc.tradeapi.v1.marketdata.BarsRequest;
import grpc.tradeapi.v1.marketdata.BarsResponse;
import grpc.tradeapi.v1.marketdata.MarketDataServiceGrpc;
import grpc.tradeapi.v1.marketdata.TimeFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.collect.prices.PricesLoader;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.properties.TrvFinamProperties;
import ru.grnk.tradevisor.common.repository.MarketDataRepository;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.FinamMetainfoRepository;

import java.time.ZonedDateTime;

import static ru.grnk.tradevisor.common.util.TimeUtils.convertToTimestamp;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinamGrpcClientService implements PricesLoader {

    public static final int MIN_TICKER_ALIVE_TIME_INTERVAL_TO_KICK = 720;
    private final TradevisorProperties properties;
    private final AssetsServiceGrpc.AssetsServiceBlockingStub assetsServiceBlockingStub;
    private final AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub;
    private final MarketDataServiceGrpc.MarketDataServiceBlockingStub marketDataServiceBlockingStub;
    private  final FinamMetainfoRepository finamMetainfoRepository;
    private final MarketDataRepository marketDataRepository;
    private final TickersRepository tickersRepository;

    public void initTickers() {
        if (tickersRepository.getProviderTickersCount("finam") > 0) return;
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

    public BearerToken getBearer() {
        TrvFinamProperties finamProperties = properties.integration().finam();
        var authRs = authServiceBlockingStub.auth(AuthRequest.newBuilder()
                .setSecret(finamProperties.secret())
                .build());
        return new BearerToken(authRs.getToken());
    }

    public void loadHistoryForSymbol(String tickerCode) {
        var symbol = tickerCode;
        log.debug("load prices for {}", symbol);
        var bearer = getBearer();
        var startTime = findStartTime(symbol, properties.integration().finam().historyMaxDepthDays());
        var endTime = convertToTimestamp(ZonedDateTime.now());
        var intervalInHours = (endTime.getSeconds() - startTime.getSeconds())/60;
        if (intervalInHours < 5) {
            return;
        }
        BarsResponse marketDataRs;
        try {
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
        } catch (Exception e) {
           log.error("ошибка временного интервала, startTime:{}, endTime:{}", startTime, endTime);
           throw new RuntimeException(e);
        }

        marketDataRs.getBarsList().stream().forEach(b -> marketDataRepository.saveMarketData(b, tickerCode));
        if(marketDataRs.getBarsList().isEmpty() && intervalInHours > MIN_TICKER_ALIVE_TIME_INTERVAL_TO_KICK) {
            tickersRepository.markTickerFailedByQuotes(tickerCode);
        }
    }

    private Timestamp  findStartTime(String symbol, int historyMaxDepthDays) {
        var res = marketDataRepository.getLatestTickTime(symbol, historyMaxDepthDays);
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
