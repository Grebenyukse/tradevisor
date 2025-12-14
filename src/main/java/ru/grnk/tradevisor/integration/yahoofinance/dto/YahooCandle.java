package ru.grnk.tradevisor.integration.yahoofinance.dto;

public record YahooCandle(
        long timestamp,
        float open,
        float high,
        float low,
        float close
) {
}
