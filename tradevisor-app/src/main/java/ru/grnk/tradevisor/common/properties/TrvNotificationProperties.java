package ru.grnk.tradevisor.common.properties;

public record TrvNotificationProperties(
        Boolean enabled,
        String cron
) {
}
