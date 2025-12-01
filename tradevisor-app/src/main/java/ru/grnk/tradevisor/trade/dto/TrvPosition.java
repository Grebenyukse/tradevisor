package ru.grnk.tradevisor.trade.dto;

import lombok.Builder;

@Builder
public record TrvPosition(
        String tickerCode,
        int direction,
        float price,
        float lot,
        float sl,
        float tp
) {
}
