package ru.grnk.tradevisor.acollect.calendar.dividends;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.acollect.EconomicEvent;
import ru.grnk.tradevisor.acollect.calendar.CalendarCollector;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;
import ru.grnk.tradevisor.zcommon.repository.TickersRepository;
import ru.tinkoff.piapi.core.InvestApi;

import java.time.Instant;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.collect.calendar.dividends.enabled")
public class DividendsCollectorImpl implements CalendarCollector {

    private final InvestApi investApi;
    private final TickersRepository tickersRepository;

    @Override
    public List<EconomicEvent> collect() {
        List<Tickers> tickers = tickersRepository.getAllTickers();
        for (var t : tickers) {
            var res = investApi.getInstrumentsService().getAssetsReportsSync(t.getUuid(), Instant.now(), Instant.now().plus(10, DAYS));
            log.info("отчет {}", res);
        }
        return List.of();
    }
}
