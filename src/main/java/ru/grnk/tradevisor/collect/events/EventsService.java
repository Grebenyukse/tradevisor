package ru.grnk.tradevisor.collect.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.common.repository.entity.Tickers;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventsService {

    private final List<EventCollector> collectors;

    public List<String> updateCalendar(Tickers ticker) {
        return collectors.stream()
                .map(l -> l.collect(ticker))
                .flatMap(List::stream)
                .toList();
    }
}
