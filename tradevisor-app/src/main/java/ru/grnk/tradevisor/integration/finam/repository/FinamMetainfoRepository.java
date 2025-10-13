package ru.grnk.tradevisor.integration.finam.repository;


import grpc.tradeapi.v1.assets.Asset;
import grpc.tradeapi.v1.assets.Exchange;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.dbmodel.tables.FinamExchanges;

import static ru.grnk.tradevisor.dbmodel.tables.FinamTickers.FINAM_TICKERS;

@Repository
@RequiredArgsConstructor
public class FinamMetainfoRepository {

    private final DSLContext dslContext;

    public void saveFinamExchange(Exchange exchange) {
        dslContext.insertInto(FinamExchanges.FINAM_EXCHANGES,
                FinamExchanges.FINAM_EXCHANGES.NAME,
                FinamExchanges.FINAM_EXCHANGES.MIC)
        .values(exchange.getName(), exchange.getMic()).onConflictDoNothing().execute();
    }

    public void saveFinamAsset(Asset asset) {
        dslContext.insertInto(FINAM_TICKERS, FINAM_TICKERS.ID, FINAM_TICKERS.TICKER, FINAM_TICKERS.MIC, FINAM_TICKERS.ISIN, FINAM_TICKERS.NAME, FINAM_TICKERS.TYPE)
                .values(asset.getId(), asset.getTicker(), asset.getMic(), asset.getIsin(), asset.getName(), asset.getType())
                .onConflictDoNothing()
                .execute();
    }
}
