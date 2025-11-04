package ru.grnk.tradevisor.common.repository;


import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.calculate.signals.TrvSignalStatus;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;
import ru.tinkoff.piapi.contract.v1.TradingStatus;

import java.util.List;
import java.util.stream.Collectors;

import static ru.grnk.tradevisor.dbmodel.tables.Signals.SIGNALS;
import static ru.grnk.tradevisor.dbmodel.tables.Tickers.TICKERS;

@Repository
@RequiredArgsConstructor
public class TickersRepository {

    private final DSLContext dsl;

    public Tickers findTickerByTickerCode(String tickerCode) {
        return dsl.select().from(TICKERS)
                .where(TICKERS.TICKER_CODE.eq(tickerCode))
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

    public List<Tickers> getUnpublishedTickers() {
        return dsl.selectFrom(TICKERS)
                .whereNotExists(
                        dsl.selectOne()
                                .from(SIGNALS)
                                .where(SIGNALS.TICKER_CODE.eq(TICKERS.TICKER_CODE))
                                .and(SIGNALS.STATUS.in(
                                        TrvSignalStatus.PUBLISHED.name(),
                                        TrvSignalStatus.CREATED.name(),
                                        TrvSignalStatus.EXECUTED.name()
                                ))
                )
                .fetchInto(Tickers.class);
    }

    public void saveInstrument(Tickers ticker) {
        dsl.insertInto(TICKERS, TICKERS.FIGI,
                        TICKERS.TICKER,
                        TICKERS.TICKER_CODE,
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
                        ticker.getTickerCode(),
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
