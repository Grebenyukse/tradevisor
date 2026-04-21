package ru.grnk.tradevisor.integration.bybit;

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
                String quoteCoin,
                String status
        ) {}
}
