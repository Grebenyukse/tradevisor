package ru.grnk.tradevisor.common.repository;


import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.FinamTickers;

import static ru.grnk.tradevisor.dbmodel.tables.FinamTickers.FINAM_TICKERS;

@Repository
@RequiredArgsConstructor
public class FinamTickersRepository {

    private final DSLContext dsl;

    public FinamTickers findFinamTickerByUuid(String uuid) {
        return dsl.select().from(FINAM_TICKERS)
                .where(FINAM_TICKERS.T_UUID.eq(uuid))
                .fetchInto(FinamTickers.class)
                .stream()
                .findFirst()
                .orElseThrow();
    };
}
