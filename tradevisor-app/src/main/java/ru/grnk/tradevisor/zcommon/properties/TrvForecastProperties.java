package ru.grnk.tradevisor.zcommon.properties;

public record TrvForecastProperties(
        String cron,
        TrvJobProperties aiscore,
        TrvJobProperties analysts
) {
}
