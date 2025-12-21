package ru.grnk.tradevisor.trade.dto;

import lombok.Builder;

@Builder
public record TrvPosition(
        String tickerCode,
        Integer direction,
        Float price,
        Float lot,
        Float sl,
        Float tp
) {
}
