package ru.grnk.tradevisor.common.properties;

public record TrvProxyApiProperties(
        Boolean enabled,
        String key,
        String url
) {
}
