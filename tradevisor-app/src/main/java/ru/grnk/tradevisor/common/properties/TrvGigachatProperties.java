package ru.grnk.tradevisor.common.properties;

public record TrvGigachatProperties(
        Boolean enabled,
        String clientId,
        String clientSecret,
        String url
) {
}
