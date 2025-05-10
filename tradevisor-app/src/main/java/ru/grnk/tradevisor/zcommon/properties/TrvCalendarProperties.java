package ru.grnk.tradevisor.zcommon.properties;

public record TrvCalendarProperties(
        Boolean economic,

        Boolean reports,
        Boolean dividends,

        Boolean expiration,
        Boolean holidays
) {
}
