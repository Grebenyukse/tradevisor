package ru.grnk.tradevisor.common.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.collect.events.TickerEvent;

import java.sql.Timestamp;

@Repository
@RequiredArgsConstructor
public class EventsRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    public void saveEvent(TickerEvent event) {
        entityManager.createNativeQuery(
                        """
                        INSERT INTO tradevisor.events (hash_id, event_date, category, source, impact, instrument_uuid, content)
                        VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)
                        ON CONFLICT (hash_id) DO NOTHING
                        """)
                .setParameter(1, event.getId())
                .setParameter(2, Timestamp.valueOf(event.getEventDate().toLocalDateTime()))
                .setParameter(3, event.getCategory().name())
                .setParameter(4, event.getSource().name())
                .setParameter(5, event.getImpact().name())
                .setParameter(6, event.getInstrumentUid())
                .setParameter(7, event.getContent())
                .executeUpdate();
    }
}
