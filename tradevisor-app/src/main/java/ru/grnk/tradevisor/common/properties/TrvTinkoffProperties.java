package ru.grnk.tradevisor.common.properties;

public record TrvTinkoffProperties(
        Boolean enabled,
        String token,
        String host,
        Integer historyMaxDepthDays
) {
}
