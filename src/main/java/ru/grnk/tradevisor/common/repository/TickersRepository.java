package ru.grnk.tradevisor.common.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.grnk.tradevisor.calculate.signals.TrvSignalStatus;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.grnk.tradevisor.common.repository.jpa.TickersJpa;
import ru.grnk.tradevisor.common.util.FuturesUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static ru.grnk.tradevisor.common.util.FuturesUtils.EXPIRATION_DATE_COMPARATOR;

@Repository
@RequiredArgsConstructor
public class TickersRepository {

    private final TickersJpa tickersRepo;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public int updateTickerSpotTickerCode(String tickerCode, String spotTickerCode) {
        return em.createQuery("UPDATE Tickers t SET t.spotTickerCode = :spotTickerCode WHERE t.tickerCode = :tickerCode")
                .setParameter("tickerCode", tickerCode)
                .setParameter("spotTickerCode", spotTickerCode)
                .executeUpdate();
    }

    public List<Tickers> findUnlinkedFutures(String provider) {
        return tickersRepo.findByProviderAndSpotTickerCodeIsNull(provider);
    }

    public Tickers findTradeTickerByTickerCode(String tickerCode) {
        return findTradeTickerByTickerCodeIfExists(tickerCode)
                .orElse(null);
    }

    public Optional<Tickers> findTradeTickerByTickerCodeIfExists(String tickerCode) {
        return em.createQuery("""
                        select t from Tickers t
                        where t.spotTickerCode = :spotTickerCode
                        order by t.ticker
                        """, Tickers.class)
                .setParameter("spotTickerCode", tickerCode)
                .getResultList()
                .stream()
                .filter(FuturesUtils::isFuturesActual)
                .min(EXPIRATION_DATE_COMPARATOR);
    }

    public Tickers getTickerByTickerCode(String tickerCode) {
        return tickersRepo.findById(tickerCode).orElseThrow();
    }

    public Optional<Tickers> findTickerByTickerCode(String tickerCode) {
        return tickersRepo.findById(tickerCode);
    }

    public List<Tickers> getAllTickers() {
        TypedQuery<Tickers> query = em.createQuery(
                "SELECT t FROM Tickers t WHERE t.status IS NULL ORDER BY t.tickerCode",
                Tickers.class
        );
        return query.getResultList();
    }

    public Integer getAllTickersCount() {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(*) FROM Tickers t WHERE t.status IS NULL AND t.provider IS NOT NULL",
                Long.class
        );
        return query.getSingleResult().intValue();
    }

    public Integer getProviderTickersCount(String provider) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(*) FROM Tickers t WHERE t.status IS NULL AND t.provider = :provider",
                Long.class
        );
        query.setParameter("provider", provider);
        return query.getSingleResult().intValue();
    }

    public List<Tickers> getAllTickers(String provider, Integer limit, Integer offset) {
        if (provider == null || provider.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider cannot be null or empty");
        }

        Query query = em.createNativeQuery("""
        SELECT t.* FROM tradevisor.tickers t
        WHERE t.status IS NULL
          AND t.provider = :provider
          AND NOT EXISTS (
            SELECT 1 FROM tradevisor.signals s 
            WHERE s.ticker_code = t.ticker_code 
              AND (
                s.created_at >= :timeBarrier 
                OR s.status IN (:activeStatuses)
              )
          )
        """, Tickers.class);
        query.setParameter("provider", provider);
        query.setParameter("activeStatuses", List.of(
                TrvSignalStatus.CREATED.name(),
                TrvSignalStatus.CONFIRMED.name(),
                TrvSignalStatus.EXECUTED.name(),
                TrvSignalStatus.MANUAL.name(),
                TrvSignalStatus.PUBLISHED.name())
        );
        query.setParameter("timeBarrier", OffsetDateTime.now().minusWeeks(2));
        query.setFirstResult(offset != null ? offset : 0);
        query.setMaxResults(limit != null ? limit : Integer.MAX_VALUE);

        return query.getResultList();
    }

    public Map<String, Integer> getTickersCountByProvider() {
        TypedQuery<Object[]> query = em.createQuery(
                "SELECT t.provider, COUNT(*) FROM Tickers t WHERE t.status IS NULL GROUP BY t.provider",
                Object[].class
        );
        return query.getResultList().stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> ((Long) row[1]).intValue()));
    }

    public int getUnpublishedTickersCount() {
        TypedQuery<Long> query = em.createQuery(
                """
                        SELECT COUNT(*) FROM Tickers t
                        WHERE NOT EXISTS (
                            SELECT 1 FROM Signals s
                            WHERE s.tickerCode = t.tickerCode
                              AND s.status IN (:statuses)
                        )
                          AND t.status IS NULL
                        """, Long.class
        );
        query.setParameter("statuses", List.of(
                TrvSignalStatus.CREATED.name(),
                TrvSignalStatus.PUBLISHED.name(),
                TrvSignalStatus.CONFIRMED.name(),
                TrvSignalStatus.EXECUTED.name(),
                TrvSignalStatus.CANCELLED.name()
        ));
        return query.getSingleResult().intValue();
    }

    public List<Tickers> getUnpublishedTickersBatch(int limit, int offset) {
        TypedQuery<Tickers> query = em.createQuery(
                """
                        SELECT t FROM Tickers t
                        WHERE NOT EXISTS (
                            SELECT 1 FROM Signals s
                            WHERE s.tickerCode = t.tickerCode
                              AND s.status IN (:statuses)
                        )
                          AND t.status IS NULL
                        ORDER BY t.tickerCode
                        """, Tickers.class
        );
        query.setParameter("statuses", List.of(
                TrvSignalStatus.CREATED.name(),
                TrvSignalStatus.PUBLISHED.name(),
                TrvSignalStatus.CONFIRMED.name(),
                TrvSignalStatus.EXECUTED.name(),
                TrvSignalStatus.CANCELLED.name()
        ));
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Transactional
    public void saveInstrument(Tickers ticker) {
        tickersRepo.upsert(
                ticker.getTickerCode(),
                ticker.getTicker(),
                ticker.getDescription(),
                ticker.getExchange(),
                ticker.getCurrency(),
                ticker.getProvider(),
                ticker.getStatus(),
                ticker.getVersion(),
                ticker.getSpotTickerCode()
        );
    }

    public int markTickerFailedByUser(String tickerCode) {
        return markTickerFailedByUser(tickerCode, "failed by user");
    }

    public int markTickerFailedByQuotes(String tickerCode) {
        return markTickerFailedByUser(tickerCode, "failed for no quotes");
    }

    public int markTickerFailedByUser(String tickerCode, String reason) {
        Optional<Tickers> opt = tickersRepo.findById(tickerCode);
        if (opt.isPresent()) {
            Tickers entity = opt.get();
            entity.setStatus(reason);
            tickersRepo.save(entity);
            return 1;
        }
        return 0;
    }
}
