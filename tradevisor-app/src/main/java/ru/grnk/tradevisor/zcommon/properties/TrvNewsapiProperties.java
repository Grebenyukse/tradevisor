package ru.grnk.tradevisor.zcommon.properties;

public record TrvNewsapiProperties(
        Boolean enabled,
        String apiKey,
        String url,
        Integer pageSize
) {
}
