package ru.grnk.tradevisor.collect.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.collect.TrvCollector;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventsService {

    private final List<EventCollector> loaders;
    private final EventsRepository eventsRepository;

    @Scheduled(cron = "${app.collect.events.cron}")
    public void updateCalendar() {
        try {
            loaders.stream()
                    .map(TrvCollector::collect)
                    .flatMap(List::stream)
                    .forEach(eventsRepository::saveEvent);
        } catch (Exception e) {
            log.error("ошибка загрузки календаря ", e);
        }
    }
}
