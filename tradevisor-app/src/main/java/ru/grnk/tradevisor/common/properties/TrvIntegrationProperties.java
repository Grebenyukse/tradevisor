package ru.grnk.tradevisor.common.properties;

public record TrvIntegrationProperties(
        TrvTinkoffProperties tinkoff,
        TrvProxyApiProperties proxyapi,
        TrvDeepseekProperties deepseek,
        TrvGigachatProperties gigachat,
        TrvTelegramProperties telegram,
        TrvNewsapiProperties newsapi,
        TrvFinamProperties finam,
        TrvBybitProperties bybit,
        TrvYahoofinanceProperties yahoofinance
) {
}
