package ru.grnk.tradevisor.integration.bybit.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;
import java.util.List;

public record BybitCandlesResponse(
        int retCode,
        String retMsg,
        Result result,
        RetExtInfo retExtInfo,
        long time
) {
    record RetExtInfo() {} // Пустой объект, если не содержит данных
    public static record Result(
            String category,
            String symbol,
            @JsonDeserialize(contentUsing = CandleDeserializer.class)
            List<Candle> list
    ) {}
    public static record Candle(
            long openTime,
            BigDecimal openPrice,
            BigDecimal highPrice,
            BigDecimal lowPrice,
            BigDecimal closePrice,
            BigDecimal volume,
            BigDecimal turnover
    ) {}
}


