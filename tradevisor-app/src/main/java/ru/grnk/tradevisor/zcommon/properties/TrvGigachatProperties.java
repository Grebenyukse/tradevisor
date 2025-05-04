package ru.grnk.tradevisor.zcommon.properties;

public record TrvGigachatProperties(
        Boolean enabled,
        String clientId,
        String clientSecret,
        String url
) {
}
