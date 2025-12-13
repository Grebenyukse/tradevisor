package ru.grnk.tradevisor.common.repository;

import com.google.protobuf.Timestamp;
import com.google.type.Decimal;
import grpc.tradeapi.v1.marketdata.Bar;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.repository.entity.MarketData;
import ru.grnk.tradevisor.common.repository.jpa.MarketDataJpa;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.Quotation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

import static ru.ttech.piapi.core.helpers.NumberMapper.quotationToBigDecimal;

@Repository
@RequiredArgsConstructor
public class MarketDataRepository {

    private final MarketDataJpa marketDataRepo;
    private final TradevisorProperties trvProperties;

    @PersistenceContext
    private EntityManager em;

    public List<MarketData> fetchMarketDataForLast(int bars, String tickerCode) {
        TypedQuery<MarketData> query = em.createQuery(
                "SELECT m FROM MarketData m WHERE m.tickerCode = :tickerCode ORDER BY m.time DESC",
                MarketData.class
        );
        query.setParameter("tickerCode", tickerCode);
        query.setMaxResults(bars);
        return query.getResultList();
    }

    public OffsetDateTime getLatestTickTime(String tickerCode, Integer historyMaxDepthDays) {
        TypedQuery<OffsetDateTime> query = em.createQuery(
                "SELECT MAX(m.time) FROM MarketData m WHERE m.tickerCode = :tickerCode",
                OffsetDateTime.class
        );
        query.setParameter("tickerCode", tickerCode);
        return query.getSingleResult();
    }

    public void saveMarketData(HistoricCandle candle, String instrument_uid) {
        MarketData entity = MarketData.builder()
                .tickerCode(instrument_uid)
                .open(floatFrom(candle.getOpen()))
                .high(floatFrom(candle.getHigh()))
                .low(floatFrom(candle.getLow()))
                .close(floatFrom(candle.getClose()))
                .time(timeFrom(candle.getTime()))
                .build();
        marketDataRepo.save(entity);
    }

    public void batchInsertMarketData(List<MarketData> records) {
        records.forEach(marketDataRepo::save);
    }

    public void saveMarketData(Bar bar, String instrument_uid) {
        MarketData entity = MarketData.builder()
                .tickerCode(instrument_uid)
                .open(floatFrom(bar.getOpen()))
                .high(floatFrom(bar.getHigh()))
                .low(floatFrom(bar.getLow()))
                .close(floatFrom(bar.getClose()))
                .time(timeFrom(bar.getTimestamp()))
                .build();
        marketDataRepo.save(entity);
    }

    public void deleteMarketData(String tickerCode) {
        marketDataRepo.deleteById(new MarketData.CompositeId(tickerCode, null)); // TODO fix composite key deletion
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

    public static float floatFrom(Decimal decimal) {
        if (decimal == null) {
            return 0.0f;
        }
        String s = decimal.getValue();
        if (s == null || s.isBlank()) {
            return 0.0f;
        }
        return new BigDecimal(s).floatValue();
    }
}
