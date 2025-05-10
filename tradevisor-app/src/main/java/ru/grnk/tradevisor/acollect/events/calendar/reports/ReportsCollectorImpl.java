package ru.grnk.tradevisor.acollect.events.calendar.reports;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.acollect.events.TickerEvent;
import ru.grnk.tradevisor.acollect.events.calendar.CalendarCollector;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;
import ru.grnk.tradevisor.zcommon.repository.TickersRepository;
import ru.tinkoff.piapi.core.InvestApi;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;
import static ru.grnk.tradevisor.zcommon.util.ObjectIdHasher.calcHash;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.collect.calendar.reports.enabled")
public class ReportsCollectorImpl implements CalendarCollector {

    private final InvestApi investApi;
    private final TickersRepository tickersRepository;

    @Override
    public List<TickerEvent> collect() {
        List<Tickers> tickers = tickersRepository.getAllTickers();
        return tickers.stream()
                .map(t -> investApi.getInstrumentsService().getAssetsReportsSync(t.getUuid(), Instant.now(), Instant.now().plus(300, DAYS)))
                .flatMap(res -> res.stream().map(x -> TickerEvent.builder()
                        .id(calcHash(x.getReportDate(), x.getInstrumentId(), "report"))
                        .eventDate(LocalDateTime.ofInstant(Instant.ofEpochSecond(x.getReportDate().getSeconds()), ZoneOffset.ofHours(3)))
                        .category("report")
                        .impact(3)
                        .country("ru")
                        .source("t-инвестиции API")
                        .tickerUid(x.getInstrumentId())
                        .title("report")
                        .description("отчетность по инструменту")
                        .build()))
                .collect(Collectors.toList());
    }
}
