package ru.grnk.tradevisor.common.properties;

public record TrvCollectProperties(
        TrvAiScoreProperties aiScore,
        TrvEventsProperties events,
        TrvPricesProperties prices
) {
}
