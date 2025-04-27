package ru.grnk.tradevisor.acollect.calendar;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.acollect.EconomicEvent;

import java.sql.Timestamp;

@Repository
@RequiredArgsConstructor
public class CalendarRepository {

    private final JdbcTemplate jdbcTemplate;
    public void saveEvent(EconomicEvent event) {
        jdbcTemplate.update(
                """
                        INSERT INTO economic_events (event_id, title, event_date, category, source,
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
                event.getTicker()
        );
    }
}
