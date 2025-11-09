package ru.grnk.tradevisor.integration.bybit;

import java.math.BigDecimal;
import java.util.List;

public record BybitTickerRs(
        int retCode,
        String retMsg,
        Result result
) {
        public record Result(
                String category,
                List<SymbolInfo> list
        ) {}

        public record SymbolInfo(
                String symbol,
                String baseCoin,
                String quoteCoin,
                String innovation,
                String status,
                String marginTrading,
                String stTag,
                LotSizeFilter lotSizeFilter,
                PriceFilter priceFilter,
                RiskParameters riskParameters,
                String symbolType
        ) {}

        public record LotSizeFilter(
                BigDecimal basePrecision,
                BigDecimal quotePrecision,
                BigDecimal minOrderQty,
                BigDecimal maxOrderQty,
                BigDecimal minOrderAmt,
                BigDecimal maxOrderAmt,
                BigDecimal maxLimitOrderQty,
                BigDecimal maxMarketOrderQty
        ) {}

        public record PriceFilter(
                BigDecimal tickSize
        ) {}

        public record RiskParameters(
                BigDecimal priceLimitRatioX,
                BigDecimal priceLimitRatioY
        ) {}
}
