package ru.grnk.tradevisor.common.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.grnk.tradevisor.calculate.signals.TrvSignalStatus;
import ru.grnk.tradevisor.calculate.strategies.dto.TrvCalculationResult;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Signals;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static ru.grnk.tradevisor.dbmodel.tables.Signals.SIGNALS;

@Repository
@RequiredArgsConstructor
public class SignalsRepository {

    private final DSLContext dsl;
    private final ObjectMapper om;

    @Transactional
    public void updateSignalStatus(Integer signalId, TrvSignalStatus status) {
        dsl.update(SIGNALS).set(SIGNALS.STATUS, status.name())
                .where(SIGNALS.ID.eq(signalId))
                .execute();
    }

    @Transactional
    public List<Signals> findUnpublishedSignals() {
        return dsl.select().from(SIGNALS)
                .where(SIGNALS.STATUS.eq(TrvSignalStatus.CREATED.name()))
                .orderBy(SIGNALS.CREATED_AT)
                .fetchStreamInto(Signals.class)
                .collect(Collectors.toList());
    }

    public Optional<Signals> findSignalBySignalId(Integer id) {
        return dsl.select().from(SIGNALS)
                .where(SIGNALS.ID.eq(id))
                .fetchOptionalInto(Signals.class);
    }

    @Transactional
    public List<Signals> findPublishedSignals() {
        return dsl.select().from(SIGNALS)
                .where(SIGNALS.STATUS.in(TrvSignalStatus.CREATED.name(), TrvSignalStatus.PUBLISHED.name()))
                .orderBy(SIGNALS.CREATED_AT)
                .fetchStreamInto(Signals.class)
                .collect(Collectors.toList());
    }

    @Transactional
    public int expirePublishedSignals(int retentionDays) {
        OffsetDateTime cutoffTime = OffsetDateTime.now().minusDays(retentionDays);
        return dsl.update(SIGNALS)
                .set(SIGNALS.STATUS, TrvSignalStatus.EXPIRED.name())
                .where(SIGNALS.STATUS.eq(TrvSignalStatus.PUBLISHED.name()))
                .and(SIGNALS.CREATED_AT.lt(cutoffTime))
                .execute();
    }

    @Transactional
    public void cancelExpiredSignals(List<Integer> ids) {
        dsl.delete(SIGNALS).where(SIGNALS.ID.in(ids)).execute();
    }

    @SneakyThrows
    @Transactional
    public void saveSignal(TrvCalculationResult trvCalculationResult,
                           String tickerCode,
                           String strategyName,
                           OffsetDateTime lastCandleTime
    ) {
        dsl.insertInto(SIGNALS,
                        SIGNALS.TICKER_CODE,
                        SIGNALS.NAME,
                        SIGNALS.DIRECTION,
                        SIGNALS.PRICE_OPEN,
                        SIGNALS.STOP_LOSS,
                        SIGNALS.TAKE_PROFIT,
                        SIGNALS.DESCRIPTION,
                        SIGNALS.STATUS,
                        SIGNALS.CREATED_AT,
                        SIGNALS.STRATEGY_PROPS
                )
                .values(
                        tickerCode,
                        strategyName,
                        trvCalculationResult.direction().directionCode(),
                        trvCalculationResult.priceOpen(),
                        trvCalculationResult.stopLoss(),
                        trvCalculationResult.takeProfit(),
                        trvCalculationResult.description(),
                        TrvSignalStatus.CREATED.name(),
                        lastCandleTime,
                        JSONB.valueOf(om.writeValueAsString(trvCalculationResult.lines()))
                )
                .onConflictDoNothing()
                .execute();
    }
}
