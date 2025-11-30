package ru.grnk.tradevisor.common.properties;

public record TrvCollectProperties(
        TrvEventsProperties events,
        TrvPricesProperties prices
) {
}
