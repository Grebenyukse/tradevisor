package ru.grnk.tradevisor.common.properties;

public record TrvFinamProperties(
        Boolean enabled,
        String secret,
        String url) {
}
