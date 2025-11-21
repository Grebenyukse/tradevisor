package ru.grnk.tradevisor.integration.yahoofinance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YahooMeta(
        String symbol,
        String currency,
        String exchangeName
) {
}
