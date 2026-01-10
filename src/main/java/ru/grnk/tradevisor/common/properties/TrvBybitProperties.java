package ru.grnk.tradevisor.common.properties;

public record TrvBybitProperties(
        Boolean enabled,
        String key,
        String secret,
        String url,
        Integer historyMaxDepthDays,
        Integer balance
        ) {
}
