package ru.grnk.tradevisor.common.repository;

import grpc.tradeapi.v1.assets.Asset;
import grpc.tradeapi.v1.assets.Exchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.common.repository.entity.FinamExchangesEntity;
import ru.grnk.tradevisor.common.repository.entity.TickersEntity;
import ru.grnk.tradevisor.common.repository.jpa.FinamExchangesJpaRepository;
import ru.grnk.tradevisor.common.repository.jpa.TickersJpaRepository;

@Repository
@RequiredArgsConstructor
public class FinamMetainfoRepository {

    private final FinamExchangesJpaRepository finamExchangesRepo;
    private final TickersJpaRepository tickersRepo;

    public void saveFinamExchange(Exchange exchange) {
        FinamExchangesEntity entity = new FinamExchangesEntity();
        entity.setName(exchange.getName());
        entity.setMic(exchange.getMic());
        finamExchangesRepo.save(entity); // onConflictDoNothing handled via DB constraint
    }

    public void saveFinamAsset(Asset asset) {
        TickersEntity entity = new TickersEntity();
        entity.setTicker(asset.getTicker());
        entity.setCurrency("rub");
        entity.setTickerCode(asset.getTicker() + "@" + asset.getMic());
        entity.setDescription(asset.getName());
        entity.setFigi(asset.getIsin());
        entity.setExchange(asset.getMic());
        entity.setProvider("finam");
        entity.setMarketType(asset.getType());

        tickersRepo.save(entity); // onConflictDoNothing handled via DB constraint
    }
}
