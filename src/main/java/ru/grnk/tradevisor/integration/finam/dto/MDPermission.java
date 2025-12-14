package ru.grnk.tradevisor.integration.finam.dto;

public record MDPermission(
        QuoteLevel quote_level,
        Integer delay_minutes,
        String mic,
        String country,
        String continent,
        Boolean worldwide
) {
}
