package ru.grnk.tradevisor.integration.finam.repository;


import grpc.tradeapi.v1.assets.Asset;
import grpc.tradeapi.v1.assets.Exchange;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.dbmodel.tables.FinamExchanges;

import static ru.grnk.tradevisor.dbmodel.Tables.TICKERS;

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
        dslContext.insertInto(TICKERS,
                        TICKERS.TICKER,
                        TICKERS.CURRENCY,
                        TICKERS.TICKER_CODE,
                        TICKERS.DESCRIPTION,
                        TICKERS.FIGI,
                        TICKERS.EXCHANGE,
                        TICKERS.PROVIDER,
                        TICKERS.MARKET_TYPE)
                .values(asset.getTicker(),
                        "rub",
                        asset.getTicker() + "@" + asset.getMic(),
                        asset.getName(),
                        asset.getIsin(),
                        asset.getMic(),
                        "finam",
                        asset.getType()
                        )
                .onConflictDoNothing()
                .execute();
    }
}
