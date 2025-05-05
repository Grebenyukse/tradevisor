package ru.grnk.tradevisor.zcommon.properties;

public record TrvCalendarProperties(
        String cron,
        TrvJobProperties economicEvent,
        TrvJobProperties dividends,
        TrvJobProperties expiration,
        TrvJobProperties holidays
) {
}
