package ru.grnk.tradevisor.zcommon.properties;

public record TrvNewsapiProperties(
        String apiKey,
        String url,
        Integer pageSize
) {
}
