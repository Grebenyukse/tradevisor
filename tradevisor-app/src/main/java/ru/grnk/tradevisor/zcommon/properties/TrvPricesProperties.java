package ru.grnk.tradevisor.zcommon.properties;

public record TrvPricesProperties(
        TrvJobProperties crypto,
        TrvJobProperties micex
) {
}
