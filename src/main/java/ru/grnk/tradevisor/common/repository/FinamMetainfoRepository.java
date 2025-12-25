package ru.grnk.tradevisor.common.repository;

import grpc.tradeapi.v1.assets.Asset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.grnk.tradevisor.common.repository.jpa.TickersJpa;

import static ru.grnk.tradevisor.collect.prices.BindTradeFuturesService.TRV_PROVIDER_TINKOFF;

@Repository
@RequiredArgsConstructor
public class FinamMetainfoRepository {

    private final TickersJpa tickersRepo;

    @Transactional
    public int saveFinamAssetIgnoringTinkoffDuplicates(Asset asset) {
        Tickers ticker = Tickers.builder()
                .ticker(asset.getTicker())
                .currency("rub")
                .tickerCode(asset.getTicker() + "@" + asset.getMic())
                .description(asset.getName())
                .exchange(asset.getMic())
                .provider("finam")
                .build();
        return tickersRepo.insertIfTickerAndProviderNotExist(
                ticker.getTickerCode(),
                ticker.getTicker(),
                ticker.getDescription(),
                ticker.getExchange(),
                ticker.getCurrency(),
                ticker.getProvider(),
                ticker.getStatus(),
                ticker.getVersion(),
                ticker.getSpotTickerCode()
                , TRV_PROVIDER_TINKOFF); // onConflictDoNothing handled via DB constraint
    }
}
