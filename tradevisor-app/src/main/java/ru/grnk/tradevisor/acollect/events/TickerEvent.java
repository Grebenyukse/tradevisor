package ru.grnk.tradevisor.acollect.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TickerEvent {
    private String id;
    private LocalDateTime eventDate;
    private String category;  // news, dividends, report, expiration
    private Integer impact;   // 1 - low, 2 - medium, 3 - high
    private String country;   // ru, us, eur ...
    private String source;    // tinkof-api, newsapi, micex, deepseek ...
    private String tickerUid; // tickers.uuid
    private String title;
    private String description;
}
