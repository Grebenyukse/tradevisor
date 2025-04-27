package ru.grnk.tradevisor.zcommon.properties;

public record TrvCalendarProperties(
        TrvJobProperties economicEvent,
        TrvJobProperties dividendsProperties,
        TrvJobProperties expiration,
        TrvJobProperties holidays
) {
}
