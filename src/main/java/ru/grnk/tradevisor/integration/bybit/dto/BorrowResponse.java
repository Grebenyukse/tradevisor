package ru.grnk.tradevisor.integration.bybit.dto;

import java.math.BigDecimal;
import java.util.List;

public record BorrowResponse(
        int retCode,
        String retMsg,
        Result result,
        RetExtInfo retExtInfo,
        long time
) {
    public static record RetExtInfo() {}
    public static record Result(
            List<BorrowItem> list
    ) {}
    public static record BorrowItem(
            BigDecimal availableToBorrow,
            String freeBorrowingAmount,
            BigDecimal freeBorrowAmount,
            BigDecimal maxBorrowingAmount,
            BigDecimal hourlyBorrowRate,
            BigDecimal borrowUsageRate,
            boolean collateralSwitch,
            BigDecimal borrowAmount,
            boolean borrowable,
            String currency,
            BigDecimal otherBorrowAmount,
            boolean marginCollateral,
            BigDecimal freeBorrowingLimit,
            BigDecimal collateralRatio
    ) {}
}

