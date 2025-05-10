package ru.grnk.tradevisor.zcommon.repository;


import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;

import java.util.List;
import java.util.stream.Collectors;

import static ru.grnk.tradevisor.dbmodel.tables.Tickers.TICKERS;

@Repository
@RequiredArgsConstructor
public class TickersRepository {

    private final DSLContext dsl;

    public Tickers findTickerByUid(String uuid) {
        return dsl.select().from(TICKERS)
                .where(TICKERS.UUID.eq(uuid))
                .fetchInto(Tickers.class)
                .stream()
                .findFirst()
                .orElseThrow();
    };

    public List<Tickers> getAllTickers() {
        return dsl.select().from(TICKERS)
                .fetchStreamInto(Tickers.class)
                .collect(Collectors.toList());
    }

    public void saveInstrument(Tickers ticker) {
        dsl.insertInto(TICKERS, TICKERS.FIGI,
                        TICKERS.TICKER,
                        TICKERS.UUID,
                        TICKERS.DESCRIPTION,
                        TICKERS.PRECISION,
                        TICKERS.GO,
                        TICKERS.LOT,
                        TICKERS.MARKET_TYPE,
                        TICKERS.EXCHANGE,
                        TICKERS.CURRENCY,
                        TICKERS.EXPIRATION
                )
                .values(ticker.getFigi(),
                        ticker.getTicker(),
                        ticker.getUuid(),
                        ticker.getDescription(),
                        ticker.getPrecision(),
                        ticker.getGo(),
                        ticker.getLot(),
                        ticker.getMarketType(),
                        ticker.getExchange(),
                        ticker.getCurrency(),
                        ticker.getExpiration()
                )
                .onConflictDoNothing()
                .execute();
    }

}
