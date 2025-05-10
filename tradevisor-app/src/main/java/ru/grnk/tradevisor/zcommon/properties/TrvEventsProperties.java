package ru.grnk.tradevisor.zcommon.properties;

public record TrvEventsProperties(
        String cron,
        TrvCalendarProperties calendar,
        TrvNewsapiProperties news
) {
}
