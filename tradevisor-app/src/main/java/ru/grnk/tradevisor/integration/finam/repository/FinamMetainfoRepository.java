package ru.grnk.tradevisor.integration.finam.repository;


import grpc.tradeapi.v1.assets.Asset;
import grpc.tradeapi.v1.assets.Exchange;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class FinamMetainfoRepository {

    private final DSLContext dslContext;

    public void saveFinamExchange(Exchange exchange) {
        dslContext.insertInto(FinamExchanges.FINAM_EXCHANGE,
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

    public List<String> getTickers() {
        return dslContext.select().from(FINAM_TICKERS).where(FINAM_TICKERS.T_UUID.eq(uid))
                .fetchSingleInto(String.class);
    }
}
