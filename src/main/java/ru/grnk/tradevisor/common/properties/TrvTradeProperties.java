package ru.grnk.tradevisor.common.properties;

public record TrvTradeProperties(
        TrvTradeLimitsProperties limits,
        Boolean enabled,
        String delay,
        RiskMoneyProperties riskMoney
) {
    public record RiskMoneyProperties(
            Integer crypto,
            Integer rus,
            Integer world
    ) {};
}
