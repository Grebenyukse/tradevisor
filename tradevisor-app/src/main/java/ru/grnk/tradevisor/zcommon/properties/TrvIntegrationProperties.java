package ru.grnk.tradevisor.zcommon.properties;

public record TrvIntegrationProperties(
        TrvTinkoffProperties tinkoff,
        TrvProxyApiProperties proxyapi,
        TrvDeepseekProperties deepseek,
        TrvGigachatProperties gigachat,
        TrvTelegramProperties telegram,
        TrvNewsapiProperties newsapi
) {
}
