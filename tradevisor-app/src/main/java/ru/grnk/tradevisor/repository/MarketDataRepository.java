package ru.grnk.tradevisor.repository;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.Quotation;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import static org.jooq.impl.DSL.max;
import static ru.grnk.tradevisor.dbmodel.tables.MarketData.MARKET_DATA;
import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;

@Repository
@RequiredArgsConstructor
public class MarketDataRepository {

    private final DSLContext dsl;

    @Value("${tinkoff.history-max-depth-days}")
    private Integer historyMaxDepthDays;

    public OffsetDateTime getLatestTickTime(String instrumentUuid) {
        return dsl.select(max(MARKET_DATA.TIME)).from(MARKET_DATA)
                .where(MARKET_DATA.INSTRUMENT_UUID.eq(instrumentUuid))
                .fetchOptionalInto(OffsetDateTime.class)
                .orElseGet(() -> OffsetDateTime.now().minusDays(historyMaxDepthDays));
    }

    public void saveMarketData(HistoricCandle candle, String instrument_uid) {
        dsl.insertInto(MARKET_DATA, MARKET_DATA.INSTRUMENT_UUID,
                MARKET_DATA.OPEN,
                MARKET_DATA.HIGH,
                MARKET_DATA.LOW,
                MARKET_DATA.CLOSE,
                MARKET_DATA.TIME
                )
                .values(
                        instrument_uid,
                        floatFrom(candle.getOpen()),
                        floatFrom(candle.getHigh()),
                        floatFrom(candle.getLow()),
                        floatFrom(candle.getClose()),
                        timeFrom(candle.getTime())
                )
                .onConflictDoNothing()
                .execute();
    }

    private static OffsetDateTime timeFrom(Timestamp timestamp) {
        return Instant.ofEpochSecond(
                timestamp.getSeconds(),
                timestamp.getNanos()
        ).atOffset(ZoneOffset.of("Europe/Moscow"));
    }

    private static Float floatFrom(Quotation quotation) {
        return Objects.requireNonNull(quotationToBigDecimal(quotation)).floatValue();
    }

}
