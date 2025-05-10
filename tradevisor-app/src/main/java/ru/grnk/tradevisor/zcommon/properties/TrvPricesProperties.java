package ru.grnk.tradevisor.zcommon.properties;

public record TrvPricesProperties(
        String cron,
        Boolean crypto,
        Boolean micex
) {
}
