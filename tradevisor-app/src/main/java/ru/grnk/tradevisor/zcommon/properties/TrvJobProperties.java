package ru.grnk.tradevisor.zcommon.properties;

public record TrvJobProperties(
        Boolean enabled,
        String cron
) {
}
