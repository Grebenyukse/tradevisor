package ru.grnk.tradevisor.integration.yahoofinance.dto;

public record YahooTickerInfo(
        String symbol,
        String name,
        String exchange,
        String type
) {
}
