package ru.grnk.tradevisor.collect.events;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
@RequiredArgsConstructor
public class EventsRepository {

    private final JdbcTemplate jdbcTemplate;
    public void saveEvent(TickerEvent event) {
        jdbcTemplate.update(
                """
                        INSERT INTO tradevisor.events (hash_id, event_date, category, source, impact, instrument_uuid, content)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                         ON CONFLICT (hash_id) DO NOTHING""",
                event.getId(),
                Timestamp.valueOf(event.getEventDate().toLocalDateTime()),
                event.getCategory().name(),
                event.getSource().name(),
                event.getImpact().name(),
                event.getInstrumentUid(),
                event.getContent()
        );
    }
}
