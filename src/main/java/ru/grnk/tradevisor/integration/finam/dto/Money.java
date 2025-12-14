package ru.grnk.tradevisor.integration.finam.dto;

public record Money(
        String currency_code,
        Long units,
        Integer nanos
) {
}
