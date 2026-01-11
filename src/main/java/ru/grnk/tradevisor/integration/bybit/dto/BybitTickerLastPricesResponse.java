package ru.grnk.tradevisor.integration.bybit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public record BybitTickerLastPricesResponse(
        @JsonProperty("retCode") int retCode,
        @JsonProperty("retMsg") String retMsg,
        @JsonProperty("result") Result result
) {
    public record Result(
            @JsonProperty("category") String category,
            @JsonProperty("list") List<Ticker> list
    ) {}

    public static record Ticker(
            @JsonProperty("symbol") String symbol,
            @JsonProperty("bid1Price") BigDecimal bid1Price,
            @JsonProperty("bid1Size") BigDecimal bid1Size,
            @JsonProperty("ask1Price") BigDecimal ask1Price,
            @JsonProperty("ask1Size") BigDecimal ask1Size,
            @JsonProperty("lastPrice") BigDecimal lastPrice,
            @JsonProperty("prevPrice24h") BigDecimal prevPrice24h,
            @JsonProperty("price24hPcnt") BigDecimal price24hPcnt,
            @JsonProperty("highPrice24h") BigDecimal highPrice24h,
            @JsonProperty("lowPrice24h") BigDecimal lowPrice24h,
            @JsonProperty("turnover24h") BigDecimal turnover24h,
            @JsonProperty("volume24h") BigDecimal volume24h,
            @JsonProperty("usdIndexPrice") BigDecimal usdIndexPrice
    ) {}
}
