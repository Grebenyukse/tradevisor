package ru.grnk.tradevisor.common.properties;

public record TrvEventsProperties(
        String cron,
        Boolean dividends,
        Boolean economic,
        Boolean expiration,
        Boolean reports,
        Boolean news
) {
}
