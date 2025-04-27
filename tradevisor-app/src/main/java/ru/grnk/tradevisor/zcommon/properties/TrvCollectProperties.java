package ru.grnk.tradevisor.zcommon.properties;

public record TrvCollectProperties(
        TrvJobProperties aiscore,
        TrvCalendarProperties calendar,
        TrvJobProperties news,
        TrvPricesProperties prices
) {
}
