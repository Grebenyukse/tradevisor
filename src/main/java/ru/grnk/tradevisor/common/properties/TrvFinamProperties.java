package ru.grnk.tradevisor.common.properties;

public record TrvFinamProperties(
        Boolean enabled,
        String secret,
        String url,
        String host,
        Integer port,
        String accountId,
        Integer historyMaxDepthDays,
        String mt5PythonClientUrl,
        String openPositionClient
        ) {
}
