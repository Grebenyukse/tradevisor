package ru.grnk.tradevisor.acollect.events.calendar;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.acollect.events.TickerEvent;

import java.sql.Timestamp;

@Repository
@RequiredArgsConstructor
public class CalendarRepository {

    private final JdbcTemplate jdbcTemplate;
    public void saveEvent(TickerEvent event) {
        jdbcTemplate.update(
                """
                        INSERT INTO tradevisor.economic_events (event_id, title, event_date, category, source,
                        impact, description, country, ticker)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                         ON CONFLICT (event_id) DO NOTHING""",
                event.getId(),
                event.getTitle(),
                Timestamp.valueOf(event.getEventDate()),
                event.getCategory(),
                event.getSource(),
                event.getImpact(),
                event.getDescription(),
                event.getCountry(),
                event.getTickerUid()
        );
    }
}
