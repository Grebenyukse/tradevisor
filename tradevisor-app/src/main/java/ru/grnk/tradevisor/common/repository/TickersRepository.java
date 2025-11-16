package ru.grnk.tradevisor.common.repository;


import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.calculate.signals.TrvSignalStatus;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.concat;
import static org.jooq.impl.DSL.inline;
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
                .where(TICKERS.STATUS.isNull())
                .orderBy(TICKERS.LOAD_PRIORITY.desc())
                .fetchStreamInto(Tickers.class)
                .collect(Collectors.toList());
    }

    public List<Tickers> getAllTickers(String provider, Integer limit) {
        return dsl.select().from(TICKERS)
                .where(TICKERS.STATUS.isNull()).and(TICKERS.PROVIDER.eq(provider))
                .orderBy(TICKERS.LOAD_PRIORITY.desc())
                .limit(limit)
                .fetchStreamInto(Tickers.class)
                .collect(Collectors.toList());
    }

    public Integer getAllTickersCount() {
        return dsl.selectCount()
                .from(TICKERS)
                .where(TICKERS.STATUS.isNull()
                        .and(TICKERS.PROVIDER.isNotNull()))
                .fetchOneInto(Integer.class);
    }

    // Новый метод с пагинацией
    public List<Tickers> getAllTickers(String provider, Integer limit, Integer offset) {
        return dsl.select().from(TICKERS)
                .where(TICKERS.STATUS.isNull()).and(TICKERS.PROVIDER.eq(provider))
                .orderBy(TICKERS.LOAD_PRIORITY.desc())
                .limit(limit)
                .offset(offset)
                .fetchStreamInto(Tickers.class)
                .collect(Collectors.toList());
    }

    public Map<String, Integer> getTickersCountByProvider() {
        return dsl.select(TICKERS.PROVIDER, DSL.count())
                .from(TICKERS)
                .where(TICKERS.STATUS.isNull())
                .groupBy(TICKERS.PROVIDER)
                .fetchMap(TICKERS.PROVIDER, DSL.count());
    }

    public List<Tickers> getUnpublishedTickers() {
        return dsl.selectFrom(TICKERS)
                .whereNotExists(
                        dsl.selectOne()
                                .from(SIGNALS)
                                .where(SIGNALS.TICKER_CODE.eq(TICKERS.TICKER_CODE))
                                .and(SIGNALS.STATUS.in(
                                        TrvSignalStatus.CREATED.name(),
                                        TrvSignalStatus.PUBLISHED.name(),
                                        TrvSignalStatus.CONFIRMED.name(),
                                        TrvSignalStatus.EXECUTED.name(),
                                        TrvSignalStatus.CANCELLED.name()
                                ))
                ).and(TICKERS.STATUS.isNull())
                .orderBy(TICKERS.LOAD_PRIORITY.desc())
                .fetchInto(Tickers.class);
    }

    // Добавьте этот метод в TickersRepository
    public int getUnpublishedTickersCount() {
        return dsl.selectCount()
                .from(TICKERS)
                .whereNotExists(
                        dsl.selectOne()
                                .from(SIGNALS)
                                .where(SIGNALS.TICKER_CODE.eq(TICKERS.TICKER_CODE))
                                .and(SIGNALS.STATUS.in(
                                        TrvSignalStatus.CREATED.name(),
                                        TrvSignalStatus.PUBLISHED.name(),
                                        TrvSignalStatus.CONFIRMED.name(),
                                        TrvSignalStatus.EXECUTED.name(),
                                        TrvSignalStatus.CANCELLED.name()
                                ))
                ).and(TICKERS.STATUS.isNull())
                .fetchOneInto(Integer.class);
    }

    // Добавьте этот метод в TickersRepository
    public List<Tickers> getUnpublishedTickersBatch(int limit, int offset) {
        return dsl.selectFrom(TICKERS)
                .whereNotExists(
                        dsl.selectOne()
                                .from(SIGNALS)
                                .where(SIGNALS.TICKER_CODE.eq(TICKERS.TICKER_CODE))
                                .and(SIGNALS.STATUS.in(
                                        TrvSignalStatus.CREATED.name(),
                                        TrvSignalStatus.PUBLISHED.name(),
                                        TrvSignalStatus.CONFIRMED.name(),
                                        TrvSignalStatus.EXECUTED.name(),
                                        TrvSignalStatus.CANCELLED.name()
                                ))
                ).and(TICKERS.STATUS.isNull())
                .orderBy(TICKERS.LOAD_PRIORITY.desc())
                .limit(limit)
                .offset(offset)
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

    public int markTickerFailedByUser(String tickerCode) {
        return markTickerFailedByUser(tickerCode, "failed by user");
    }

    public int  markTickerFailedByQuotes(String tickerCode) {
        return markTickerFailedByUser(tickerCode, "failed for no quotes");
    }

    public int markTickerFailedByUser(String tickerCode, String reason) {
        return dsl.update(TICKERS)
                .set(TICKERS.STATUS, reason)
                .where(TICKERS.TICKER_CODE.eq(tickerCode))
                .execute();
    }

}
