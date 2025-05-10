package ru.grnk.tradevisor.zcommon.properties;

public record TrvCollectProperties(
        TrvEventsProperties events,

        TrvForecastProperties forecast,
        TrvPricesProperties prices
) {
}
