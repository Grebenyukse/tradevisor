package ru.grnk.tradevisor.acollect;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EconomicEvent {
    private String id;
    private LocalDateTime eventDate;
    private String category;  // CRYPTO, STOCK, ETF, FOREX, FUTURES, IPO, ECONOMIC
    private Integer impact;   // 1 - low, 2 - medium, 3 - high
    private String country;
    private String source;
    private String ticker;
    private String title;
    private String description;
}
