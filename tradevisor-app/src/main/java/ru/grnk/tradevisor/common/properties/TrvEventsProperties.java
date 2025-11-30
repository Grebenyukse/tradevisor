package ru.grnk.tradevisor.common.properties;

public record TrvEventsProperties(
        Boolean dividends,
        Boolean economic,
        Boolean expiration,
        Boolean reports,
        Boolean news
) {
}
