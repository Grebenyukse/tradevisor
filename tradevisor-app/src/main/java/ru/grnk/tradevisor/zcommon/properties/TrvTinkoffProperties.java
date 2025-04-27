package ru.grnk.tradevisor.zcommon.properties;

public record TrvTinkoffProperties(
        String token,
        String host,
        Integer historyMaxDepthDays
) {
}
