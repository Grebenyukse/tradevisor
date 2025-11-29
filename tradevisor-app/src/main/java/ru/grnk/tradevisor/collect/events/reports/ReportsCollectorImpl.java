package ru.grnk.tradevisor.collect.events.reports;

import com.google.protobuf.AbstractMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.collect.events.EventCollector;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;
import ru.tinkoff.piapi.core.InvestApi;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.collect.calendar.reports.enabled")
public class ReportsCollectorImpl implements EventCollector {

    private final InvestApi investApi;

    @Override
    public List<String> collect(Tickers ticker) {
        if (!Objects.equals(ticker.getProvider(), "finam")) return List.of();
        return investApi.getInstrumentsService()
                .getAssetsReportsSync(ticker.getTickerCode())
                .stream()
                .map(AbstractMessage::toString)
                .toList();
    }
}
