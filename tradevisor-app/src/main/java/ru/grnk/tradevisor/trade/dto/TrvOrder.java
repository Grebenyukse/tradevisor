package ru.grnk.tradevisor.trade.dto;

import lombok.Builder;

@Builder
public record TrvOrder(
        String tickerCode,
        int direction,
        float activation,
        float price,
        float lot,
        boolean isGtc
) {
}
