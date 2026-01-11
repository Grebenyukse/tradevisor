package ru.grnk.tradevisor.integration.bybit.dto;

import java.math.BigDecimal;
import java.util.List;

public record PositionResponse(
        int retCode,
        String retMsg,
        PositionResult result,
        RetExtInfo retExtInfo,
        long time
) {
    public static record RetExtInfo() {}
    public static record PositionResult(
            String nextPageCursor,
            String category,
            List<PositionItem> list
    ) {}
    public static record PositionItem(
            String symbol,
            BigDecimal leverage,
            int autoAddMargin,
            BigDecimal avgPrice,
            BigDecimal liqPrice,
            BigDecimal riskLimitValue,
            String takeProfit,
            BigDecimal positionValue,
            boolean isReduceOnly,
            BigDecimal positionIMByMp,
            String tpslMode,
            int riskId,
            BigDecimal trailingStop,
            String liqPriceByMp,
            BigDecimal unrealisedPnl,
            BigDecimal markPrice,
            int adlRankIndicator,
            BigDecimal cumRealisedPnl,
            BigDecimal positionMM,
            String createdTime,
            int positionIdx,
            BigDecimal positionIM,
            BigDecimal positionMMByMp,
            long seq,
            String updatedTime,
            String side,
            String bustPrice,
            BigDecimal positionBalance,
            String leverageSysUpdatedTime,
            BigDecimal curRealisedPnl,
            BigDecimal size,
            String positionStatus,
            String mmrSysUpdatedTime,
            String stopLoss,
            int tradeMode,
            String sessionAvgPrice
    ) {}
}
