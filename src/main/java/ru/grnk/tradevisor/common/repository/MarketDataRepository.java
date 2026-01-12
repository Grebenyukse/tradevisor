package ru.grnk.tradevisor.common.repository;

import com.google.protobuf.Timestamp;
import com.google.type.Decimal;
import grpc.tradeapi.v1.marketdata.Bar;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
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

import static java.util.Optional.ofNullable;
import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;

@Repository
@RequiredArgsConstructor
@Slf4j
public class MarketDataRepository {

    private final MarketDataJpa marketDataRepo;

    @PersistenceContext
    private EntityManager em;

    public float getTickerByTickerRelation(String tickerCode, String baseTickerCode) {
        var res = em.createQuery("""
        SELECT md1.tickerCode, md2.tickerCode, md1.close, md2.close,
               CASE WHEN md2.close != 0 THEN md1.close / md2.close ELSE NULL END,
               md1.time
        FROM MarketData md1, MarketData md2 
        WHERE md1.tickerCode = :ticker_code 
          AND md2.tickerCode = :ticker_code_base 
          AND md1.time = md2.time
        ORDER BY md1.time DESC
        """, TickersRation.class)
                .setParameter("ticker_code", tickerCode)
                .setParameter("ticker_code_base", baseTickerCode)
                .setMaxResults(1)
                .getSingleResult();
        log.info("relation of tickers. ticker: {}, baseTicker: {}, ticker_close_price: {}, base_ticker_close_price:{}, time: {}, ratio: {}",
                res.ticker(), res.baseTicker(), res.tickerClose(), res.baseTickerClose(), res.time(), res.ratio());
        return res.ratio();
    }

    private record TickersRation(
            String ticker,
            String baseTicker,
            Float tickerClose,     // Changed from String to Float
            Float baseTickerClose, // Same here
            Float ratio,           // This should be Float or double depending on division precision
            OffsetDateTime time   // Was String before; now correct
    ) {}

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
        var minEndTime = OffsetDateTime.now().minusDays(historyMaxDepthDays);
        return ofNullable(query.getSingleResult())
                .filter(x -> x.getSecond() > minEndTime.getSecond())
                .orElse(minEndTime);
    }

    @Transactional
    public void saveMarketData(HistoricCandle candle, String instrument_uid) {
        marketDataRepo.insertOverwrite(instrument_uid,
                timeFrom(candle.getTime()),
                floatFrom(candle.getOpen()),
                floatFrom(candle.getHigh()),
                floatFrom(candle.getLow()),
                floatFrom(candle.getClose())
        );
    }

    @Transactional
    public void batchInsertMarketData(List<MarketData> records) {
        records.forEach(x -> marketDataRepo.insertOverwrite(
                x.getTickerCode(),
                x.getTime(),
                x.getOpen(),
                x.getHigh(),
                x.getLow(),
                x.getClose()
        ));
    }

    @Transactional
    public void saveMarketData(Bar bar, String instrument_uid) {
        marketDataRepo.insertOverwrite(
                instrument_uid,
                timeFrom(bar.getTimestamp()),
                floatFrom(bar.getOpen()),
                floatFrom(bar.getHigh()),
                floatFrom(bar.getLow()),
                floatFrom(bar.getClose())
        );
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
