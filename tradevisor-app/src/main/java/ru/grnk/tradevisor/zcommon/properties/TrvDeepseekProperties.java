package ru.grnk.tradevisor.zcommon.properties;

public record TrvDeepseekProperties(
        Boolean enabled,
        String key,
        String url
) {
}
