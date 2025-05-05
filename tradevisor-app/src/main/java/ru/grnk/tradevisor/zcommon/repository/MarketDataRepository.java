package ru.grnk.tradevisor.zcommon.repository;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.MarketData;
import ru.grnk.tradevisor.zcommon.properties.TradevisorProperties;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.Quotation;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.max;
import static ru.grnk.tradevisor.dbmodel.tables.MarketData.MARKET_DATA;
import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;

@Repository
@RequiredArgsConstructor
public class MarketDataRepository {

    private final DSLContext dsl;
    private final TradevisorProperties trvProperties;

    public List<MarketData> fetchMarketDataForLast(int bars, String instrumentUid) {
        return dsl.select().from(MARKET_DATA)
                .where(MARKET_DATA.INSTRUMENT_UUID.eq(instrumentUid))
                .orderBy(MARKET_DATA.ID.desc())
                .limit(bars).offset(0)
                .fetchStreamInto(MarketData.class)
                .collect(Collectors.toList());
    }

    public OffsetDateTime getLatestTickTime(String instrumentUuid) {
        var historyMaxDepthDays = trvProperties.integration().tinkoff().historyMaxDepthDays();
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
        ).atZone(ZoneId.of("Europe/Moscow")).toOffsetDateTime();
    }

    private static Float floatFrom(Quotation quotation) {
        return Objects.requireNonNull(quotationToBigDecimal(quotation)).floatValue();
    }

}
