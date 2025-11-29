package ru.grnk.tradevisor.collect.events.economic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.collect.events.EventCollector;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Tickers;
import ru.grnk.tradevisor.integration.ai.AskAiModel;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EconomicEventCollectorImpl implements EventCollector {

    private final List<AskAiModel> loaders;

    public static final String TRV_CALENDAR_PROMPT = """
            Use www.investing.com/economic-calendar. Return JSON array with objects containing: impact (1-3),
            country (eur/usa/china/russia), tickers[], title, description (<10 words), event_date (YYYY-MM-DD).
            Dates: %s to %s. Example: [{"impact":3,"country":"usa","tickers":["DXY"],"title":"Nonfarm Payrolls",
            "description":"Monthly jobs report", "event_date": "2025-05-06"}]
            """;

    @Override
    public List<String> collect(Tickers ticker) {
        return loaders.stream()
                .map(x -> x.ask(TRV_CALENDAR_PROMPT, 3))
                .collect(Collectors.toList());
    }
}
