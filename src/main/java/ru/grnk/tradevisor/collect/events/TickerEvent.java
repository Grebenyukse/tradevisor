package ru.grnk.tradevisor.collect.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.grnk.tradevisor.collect.TrvSource;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TickerEvent {
    private String id;                  // id события
    private OffsetDateTime eventDate;   // дата наступления события (новости - в прошлом, календарь - в будущем)
    private EventCategory category;     // категория события (дивиденды, новости, отчет и тд...)
    private EventImpact impact;         // влияние на инструмент
    private TrvSource source;           // tinkof-api, newsapi, micex, deepseek ...
    private String instrumentUid;       // tickers.uuid
    private String content;             // содержание события
}
