package ru.grnk.tradevisor.integration.rts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Record-based DTO for parsing MOEX API response for futures contract data.
 * This represents the structure returned by:
 * https://iss.moex.com/iss/engines/futures/markets/forts/securities.json?secid=TICKER
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MoexFuturesResponse(
    Securities securities
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Securities(
        List<String> columns,
        List<List<Object>> data
    ) {}
}