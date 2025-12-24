package ru.grnk.tradevisor.trade.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record TrvOrder(
        String tickerCode,
        int direction,
        BigDecimal activation,
        BigDecimal price,
        int lot,
        boolean isGtc,
        String status
) {
}
