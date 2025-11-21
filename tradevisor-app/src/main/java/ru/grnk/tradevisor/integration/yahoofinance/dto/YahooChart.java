package ru.grnk.tradevisor.integration.yahoofinance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YahooChart(List<YahooChartResult> result, YahooChartError error) {
}
