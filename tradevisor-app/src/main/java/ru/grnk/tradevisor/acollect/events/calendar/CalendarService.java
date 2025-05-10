package ru.grnk.tradevisor.acollect.events.calendar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.acollect.TrvCollector;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService {

    private final List<CalendarCollector> loaders;
    private final CalendarRepository calendarRepository;

    @Scheduled(cron = "${app.collect.events.cron}")
    public void updateCalendar() {
        try {
            loaders.stream()
                    .map(TrvCollector::collect)
                    .flatMap(List::stream)
                    .forEach(calendarRepository::saveEvent);
        } catch (Exception e) {
            log.error("ошибка загрузки календаря ", e);
        }
    }
}
