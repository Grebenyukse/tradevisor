package ru.grnk.tradevisor.zcommon.properties;

public record TrvCalendarProperties(
        TrvJobProperties economic,

        TrvJobProperties reports,
        TrvJobProperties dividends,

        TrvJobProperties expiration,
        TrvJobProperties holidays
) {
}
