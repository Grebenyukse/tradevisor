package ru.grnk.tradevisor.common.properties;

public record TrvTradeProperties(
        TrvTradeLimitsProperties limits,
        Boolean enabled,
        String delay
) {
}
