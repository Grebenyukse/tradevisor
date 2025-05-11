package ru.grnk.tradevisor.collect.events.expiration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.collect.TrvSource;
import ru.grnk.tradevisor.collect.events.EventCollector;
import ru.grnk.tradevisor.collect.events.dto.EventCategory;
import ru.grnk.tradevisor.collect.events.dto.EventImpact;
import ru.grnk.tradevisor.collect.events.dto.TickerEvent;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;
import ru.tinkoff.piapi.core.InvestApi;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;
import static ru.grnk.tradevisor.common.util.ObjectIdHasher.calcHash;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.collect.calendar.events.expiration.enabled")
public class ExpirationCollectorImpl implements EventCollector {

    private final InvestApi investApi;
    private final TickersRepository tickersRepository;

    @Override
    public List<TickerEvent> collect() {
        List<Tickers> tickers = tickersRepository.getAllTickers();
        return tickers.stream()
                .map(t -> investApi.getInstrumentsService().getAssetsReportsSync(t.getUuid(), Instant.now(), Instant.now().plus(300, DAYS)))
                .flatMap(res -> res.stream().map(x -> TickerEvent.builder()
                        .id(calcHash(x.getReportDate(), x.getInstrumentId(), EventCategory.EXPIRATION.name()))
                        .eventDate(OffsetDateTime.ofInstant(Instant.ofEpochSecond(x.getReportDate().getSeconds()), ZoneOffset.ofHours(3)))
                        .category(EventCategory.EXPIRATION)
                        .impact(EventImpact.HIGH)
                        .source(TrvSource.TINKOFF_API)
                        .tickerUid(x.getInstrumentId())
                        .content("report" + " " + "отчетность по инструменту")
                        .build()))
                .collect(Collectors.toList());
    }
}
