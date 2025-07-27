package ru.grnk.tradevisor.integration.finam.dto;

public record Position(
        String symbol,
        String quantity,
        String average_price,
        String current_price
) {
}