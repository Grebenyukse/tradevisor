package ru.grnk.tradevisor.zcommon.properties;

public record TrvForecastProperties(
        String cron,
        Boolean aiscore,
        Boolean analysts
) {
}
