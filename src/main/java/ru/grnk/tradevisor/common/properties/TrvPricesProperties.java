package ru.grnk.tradevisor.common.properties;

public record TrvPricesProperties(
        String delay,
        Boolean finam,
        Boolean tinkoff,
        Boolean bybit,
        Boolean yahoofinance,
        Boolean world,
        InitTickersProperties initTickers
) {
}
