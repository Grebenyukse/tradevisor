package ru.grnk.tradevisor.common.properties;

public record TrvPricesProperties(
        String cron,
        Boolean crypto,
        Boolean micex
) {
}
