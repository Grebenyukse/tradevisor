package ru.grnk.tradevisor.integration.yahoofinance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YahooIndicators(List<YahooQuote> quote) {
}
