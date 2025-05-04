package ru.grnk.tradevisor.zcommon.properties;

public record TrvProxyApiProperties(
        Boolean enabled,
        String key,
        String url
) {
}
