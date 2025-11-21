package ru.grnk.tradevisor.integration.yahoofinance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YahooQuote(
        List<BigDecimal> open,
        List<BigDecimal> high,
        List<BigDecimal> low,
        List<BigDecimal> close,
        List<BigDecimal> volume
) {
}
