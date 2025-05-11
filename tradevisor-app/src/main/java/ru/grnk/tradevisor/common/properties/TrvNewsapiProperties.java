package ru.grnk.tradevisor.common.properties;

public record TrvNewsapiProperties(
        Boolean enabled,
        String apiKey,
        String url,
        Integer pageSize
) {
}
