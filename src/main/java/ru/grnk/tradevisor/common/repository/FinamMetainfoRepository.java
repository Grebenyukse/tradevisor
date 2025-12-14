package ru.grnk.tradevisor.common.repository;

import grpc.tradeapi.v1.assets.Asset;
import grpc.tradeapi.v1.assets.Exchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.common.repository.entity.FinamExchanges;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.grnk.tradevisor.common.repository.jpa.FinamExchangesJpa;
import ru.grnk.tradevisor.common.repository.jpa.TickersJpa;

@Repository
@RequiredArgsConstructor
public class FinamMetainfoRepository {

    private final FinamExchangesJpa finamExchangesRepo;
    private final TickersJpa tickersRepo;

    public void saveFinamExchange(Exchange exchange) {
        FinamExchanges entity = FinamExchanges.builder()
                .name(exchange.getName())
                .mic(exchange.getMic())
                .build();
        finamExchangesRepo.save(entity); // onConflictDoNothing handled via DB constraint
    }

    public void saveFinamAsset(Asset asset) {
        Tickers entity = Tickers.builder()
                .ticker(asset.getTicker())
                .currency("rub")
                .tickerCode(asset.getTicker() + "@" + asset.getMic())
                .description(asset.getName())
                .figi(asset.getIsin())
                .exchange(asset.getMic())
                .provider("finam")
                .marketType(asset.getType())
                .build();
        tickersRepo.save(entity); // onConflictDoNothing handled via DB constraint
    }
}
