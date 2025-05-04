package ru.grnk.tradevisor.zcommon.properties;

public record TrvTinkoffProperties(
        Boolean enabled,
        String token,
        String host,
        Integer historyMaxDepthDays
) {
}
