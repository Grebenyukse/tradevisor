package ru.grnk.tradevisor.integration.tinkoff.dto;


import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record TradeSignal(
         Integer id,
         String name,
         String description,
         String tickerCode,
         Short direction,
         BigDecimal priceOpen,
         BigDecimal stopLoss,
         BigDecimal takeProfit,
         OffsetDateTime createdAt,
         OffsetDateTime updatedAt,
         String status
) {
}
