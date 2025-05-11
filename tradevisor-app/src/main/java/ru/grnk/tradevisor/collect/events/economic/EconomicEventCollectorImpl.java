package ru.grnk.tradevisor.collect.events.economic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.collect.TrvSource;
import ru.grnk.tradevisor.collect.events.dto.EventCategory;
import ru.grnk.tradevisor.collect.events.dto.EventImpact;
import ru.grnk.tradevisor.collect.events.dto.TickerEvent;
import ru.grnk.tradevisor.collect.events.EventCollector;
import ru.grnk.tradevisor.integration.ai.AskAiModel;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static ru.grnk.tradevisor.common.util.ObjectIdHasher.calcHash;
import static ru.grnk.tradevisor.common.util.ObjectMapperUtils.readValue;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.collect.calendar.economic-event.enabled")
public class EconomicEventCollectorImpl implements EventCollector {

    private final List<AskAiModel> loaders;

    public static final String TRV_CALENDAR_PROMPT = """
            Use www.investing.com/economic-calendar. Return JSON array with objects containing: impact (1-3),
            country (eur/usa/china/russia), tickers[], title, description (<10 words), event_date (YYYY-MM-DD).
            Dates: %s to %s. Example: [{"impact":3,"country":"usa","tickers":["DXY"],"title":"Nonfarm Payrolls",
            "description":"Monthly jobs report", "event_date": "2025-05-06"}]
            """;

    @Override
    public List<TickerEvent> collect() {
        return loaders.stream()
                .map(x -> x.ask(TRV_CALENDAR_PROMPT, 3))
                .map(x -> Arrays.asList(readValue(x, AiCalendarResponseDto[].class)))
                .flatMap(List::stream)
                .flatMap(x -> x.tickers().stream()
                        .map(y -> TickerEvent.builder()
                                .id(calcHash(x.title(), x.description(), x.event_date(), y, x.impact()))
                                .eventDate(OffsetDateTime.of(LocalDateTime.parse(x.event_date()), ZoneOffset.of("Moscow")))
                                .category(EventCategory.ECONOMIC)
                                .impact(EventImpact.HIGH)
                                .source(TrvSource.AI)
                                .tickerUid(y)
                                .content(x.title() + " " + x.description())
                                .build()))
                .collect(Collectors.toList());
    }
}
