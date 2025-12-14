package ru.grnk.tradevisor.common.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.notify.plot.dto.OHLCData;
import ru.grnk.tradevisor.notify.plot.dto.PlotRecord;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PlotRepository {

    @PersistenceContext
    private EntityManager em;

    public List<PlotRecord> getTickerPlotInfo(String uuid) {
        TypedQuery<OHLCData> query = em.createQuery(
                "SELECT new ru.grnk.tradevisor.notify.plot.dto.OHLCData(" +
                        "m.open, m.high, m.low, m.close, m.time)" +
                        "FROM MarketData m WHERE m.tickerCode = :uuid",
                OHLCData.class
        );
        query.setParameter("uuid", uuid);
        List<OHLCData> ohlcList = query.getResultList();

        // Здесь можно добавить логику получения tickerInfo через Tickers
        // Но пока просто возвращаем пустой список, как было ранее
        return List.of(); // Заглушка
    }
}
