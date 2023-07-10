package ru.grnk.tradevisor.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import ru.grnk.tradevisor.model.signals.TrvSignalStatus;
import ru.grnk.tradevisor.model.strategies.TrvCalculationResult;

import java.time.OffsetDateTime;

import static ru.grnk.tradevisor.dbmodel.tables.Signals.SIGNALS;

@RequiredArgsConstructor
public class SignalsRepository {

    private final DSLContext dsl;

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
