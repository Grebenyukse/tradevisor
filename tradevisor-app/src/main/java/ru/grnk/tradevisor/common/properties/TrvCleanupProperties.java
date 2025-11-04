package ru.grnk.tradevisor.common.properties;

public record TrvCleanupProperties(
        String cron,
        Boolean enabled,
        Integer retentionDays
) {
}
