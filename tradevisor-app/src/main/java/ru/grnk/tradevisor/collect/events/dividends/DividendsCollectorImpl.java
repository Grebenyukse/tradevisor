package ru.grnk.tradevisor.collect.events.dividends;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.collect.events.EventCollector;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;
import ru.tinkoff.piapi.core.InvestApi;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.collect.events.dividends")
public class DividendsCollectorImpl implements EventCollector {

    private final InvestApi investApi;
    private final TickersRepository tickersRepository;

    @Override
    public List<String> collect(Tickers ticker) {
        return List.of();
    }
}