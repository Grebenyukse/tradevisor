package ru.grnk.tradevisor.common.properties;

public record TrvDeepseekProperties(
        Boolean enabled,
        String key,
        String url
) {
}
