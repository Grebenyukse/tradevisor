package ru.grnk.tradevisor.common.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.calculate.signals.TrvSignalStatus;
import ru.grnk.tradevisor.common.repository.entity.Tickers;
import ru.grnk.tradevisor.common.repository.jpa.TickersJpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TickersRepository {

    private final TickersJpa tickersRepo;

    @PersistenceContext
    private EntityManager em;

    public Tickers findTickerByTickerCode(String tickerCode) {
        return tickersRepo.findById(tickerCode).orElseThrow();
    }

    public List<Tickers> getAllTickers() {
        TypedQuery<Tickers> query = em.createQuery(
                "SELECT t FROM Tickers t WHERE t.status IS NULL ORDER BY t.loadPriority DESC",
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
        TypedQuery<Tickers> query = em.createQuery(
                "SELECT t FROM Tickers t WHERE t.status IS NULL AND t.provider = :provider ORDER BY t.loadPriority DESC",
                Tickers.class
        );
        query.setParameter("provider", provider);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    public Map<String, Integer> getTickersCountByProvider() {
        TypedQuery<Object[]> query = em.createQuery(
                "SELECT t.provider, COUNT(*) FROM Tickers t WHERE t.status IS NULL GROUP BY t.provider",
                Object[].class
        );
        return query.getResultList().stream()
                .collect(Collectors.toMap(row -> (String)row[0], row -> ((Long)row[1]).intValue()));
    }

    public int getUnpublishedTickersCount() {
        TypedQuery<Long> query = em.createQuery(
                """
                SELECT COUNT(*) FROM Tickers t
                WHERE NOT EXISTS (
                    SELECT 1 FROM SignalsEntity s
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
                    SELECT 1 FROM SignalsEntity s
                    WHERE s.tickerCode = t.tickerCode
                      AND s.status IN (:statuses)
                )
                  AND t.status IS NULL
                ORDER BY t.loadPriority DESC
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

    public void saveInstrument(Tickers ticker, String provider) {
        ticker.setProvider(provider);
        tickersRepo.save(ticker);
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
