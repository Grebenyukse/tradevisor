package ru.grnk.tradevisor.trade.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record TrvPosition(
        String tickerCode,
        Integer direction,
        BigDecimal price,
        int lot,
        BigDecimal sl,
        BigDecimal tp
) {
}
