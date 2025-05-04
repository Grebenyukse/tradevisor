package ru.grnk.tradevisor.acollect.calendar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.acollect.EconomicEvent;
import ru.grnk.tradevisor.acollect.calendar.dto.AiCalendarResponse;
import ru.grnk.tradevisor.eintegration.ai.AskAiModel;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static ru.grnk.tradevisor.zcommon.util.ObjectIdHasher.calcHash;
import static ru.grnk.tradevisor.zcommon.util.ObjectMapperUtils.readValue;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.collect.calendar.economic-event.enabled")
public class CalendarService {

    private final List<AskAiModel> loaders;
    private final CalendarRepository calendarRepository;

    public static final String TRV_CALENDAR_PROMPT = """
            Use www.investing.com/economic-calendar. Return JSON array with objects containing: impact (1-3),
            country (eur/usa/china/russia), tickers[], title, description (<10 words), event_date (YYYY-MM-DD).
            Dates: %s to %s. Example: [{"impact":3,"country":"usa","tickers":["DXY"],"title":"Nonfarm Payrolls",
            "description":"Monthly jobs report", "event_date": "2025-05-06"}]
            """;

    @Scheduled(cron = "${collect.calendar.update.cron}")
    public void updateCalendar() {
        try {
            loaders.stream()
                    .map(x -> x.ask(TRV_CALENDAR_PROMPT, 3))
                    .map(x -> Arrays.asList(readValue(x, AiCalendarResponse[].class)))
                    .flatMap(List::stream)
                    .flatMap(x -> x.tickers().stream()
                            .map(y -> EconomicEvent.builder()
                                    .id(calcHash(x.title(), x.description(), x.event_date(), y, x.impact()))
                                    .eventDate(LocalDateTime.parse(x.event_date()))
                                    .category("economic event")
                                    .impact(x.impact())
                                    .country(x.country())
                                    .source("investing")
                                    .ticker(y)
                                    .title(x.title())
                                    .description(x.description())
                                    .build()))
                    .forEach(calendarRepository::saveEvent);
        } catch (Exception e) {
            log.error("ошибка загрузки календаря ", e);
        }
    }
}
