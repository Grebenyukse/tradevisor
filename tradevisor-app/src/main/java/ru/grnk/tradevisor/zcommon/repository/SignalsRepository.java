package ru.grnk.tradevisor.zcommon.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Signals;
import ru.grnk.tradevisor.zcommon.model.signals.TrvSignalStatus;
import ru.grnk.tradevisor.zcommon.model.strategies.TrvCalculationResult;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static ru.grnk.tradevisor.dbmodel.tables.Signals.SIGNALS;

@Repository
@RequiredArgsConstructor
public class SignalsRepository {

    private final DSLContext dsl;

    @Transactional
    public void updateSignalStatus(Signals signal, TrvSignalStatus status) {
        dsl.update(SIGNALS).set(SIGNALS.STATUS, status.name())
                .where(SIGNALS.ID.eq(signal.getId()))
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

    @Transactional
    public void saveSignal(TrvCalculationResult trvCalculationResult,
                           String instrumentUid,
                           String strategyName,
                           OffsetDateTime lastCandleTime
    ) {
        dsl.insertInto(SIGNALS,
                        SIGNALS.INSTRUMENT_UUID,
                        SIGNALS.NAME,
                        SIGNALS.DIRECTION,
                        SIGNALS.PRICE_OPEN,
                        SIGNALS.STOP_LOSS,
                        SIGNALS.TAKE_PROFIT,
                        SIGNALS.DESCRIPTION,
                        SIGNALS.STATUS,
                        SIGNALS.CREATED_AT
                )
                .values(
                        instrumentUid,
                        strategyName,
                        trvCalculationResult.direction().directionCode(),
                        trvCalculationResult.priceOpen(),
                        trvCalculationResult.stopLoss(),
                        trvCalculationResult.takeProfit(),
                        null,
                        TrvSignalStatus.CREATED.name(),
                        lastCandleTime
                )
                .onConflictDoNothing()
                .execute();
    }
}
