package ru.grnk.tradevisor.collect.events;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.collect.events.dto.TickerEvent;

import java.sql.Timestamp;

@Repository
@RequiredArgsConstructor
public class EventsRepository {

    private final JdbcTemplate jdbcTemplate;
    public void saveEvent(TickerEvent event) {
        jdbcTemplate.update(
                """
                        INSERT INTO tradevisor.economic_events (event_id, event_date, category, source, impact, ticker_uid, content)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                         ON CONFLICT (event_id) DO NOTHING""",
                event.getId(),
                Timestamp.valueOf(event.getEventDate().toLocalDateTime()),
                event.getCategory(),
                event.getSource(),
                event.getImpact(),
                event.getTickerUid(),
                event.getContent()
        );
    }
}
