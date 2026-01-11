package ru.grnk.tradevisor.integration.bybit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public record BybitAccountInfoResponse(
        @JsonProperty("retCode") int retCode,
        @JsonProperty("retMsg") String retMsg,
        @JsonProperty("result") AccountResult result,
        @JsonProperty("retExtInfo") RetExtInfo retExtInfo,
        @JsonProperty("time") long time
) {
    public record AccountResult(
            @JsonProperty("list") List<AccountInfo> list
    ) {}

    public record AccountInfo(
            @JsonProperty("accountIMRate") BigDecimal accountIMRate,
            @JsonProperty("totalMaintenanceMarginByMp") BigDecimal totalMaintenanceMarginByMp,
            @JsonProperty("totalInitialMargin") BigDecimal totalInitialMargin,
            @JsonProperty("accountType") String accountType,
            @JsonProperty("accountMMRate") BigDecimal accountMMRate,
            @JsonProperty("accountMMRateByMp") BigDecimal accountMMRateByMp,
            @JsonProperty("accountIMRateByMp") BigDecimal accountIMRateByMp,
            @JsonProperty("totalInitialMarginByMp") BigDecimal totalInitialMarginByMp,
            @JsonProperty("totalMaintenanceMargin") BigDecimal totalMaintenanceMargin,
            @JsonProperty("totalEquity") BigDecimal totalEquity,
            @JsonProperty("totalMarginBalance") BigDecimal totalMarginBalance,
            @JsonProperty("totalAvailableBalance") BigDecimal totalAvailableBalance,
            @JsonProperty("totalPerpUPL") BigDecimal totalPerpUPL,
            @JsonProperty("totalWalletBalance") BigDecimal totalWalletBalance,
            @JsonProperty("accountLTV") BigDecimal accountLTV,
            @JsonProperty("coin") List<CoinInfo> coin
    ) {}

    public record CoinInfo(
            @JsonProperty("spotBorrow") BigDecimal spotBorrow,
            @JsonProperty("availableToBorrow") String availableToBorrow,
            @JsonProperty("bonus") BigDecimal bonus,
            @JsonProperty("accruedInterest") BigDecimal accruedInterest,
            @JsonProperty("availableToWithdraw") String availableToWithdraw,
            @JsonProperty("totalOrderIM") BigDecimal totalOrderIM,
            @JsonProperty("equity") BigDecimal equity,
            @JsonProperty("totalPositionMM") BigDecimal totalPositionMM,
            @JsonProperty("usdValue") BigDecimal usdValue,
            @JsonProperty("unrealisedPnl") BigDecimal unrealisedPnl,
            @JsonProperty("collateralSwitch") boolean collateralSwitch,
            @JsonProperty("spotHedgingQty") BigDecimal spotHedgingQty,
            @JsonProperty("borrowAmount") BigDecimal borrowAmount,
            @JsonProperty("totalPositionIM") BigDecimal totalPositionIM,
            @JsonProperty("walletBalance") BigDecimal walletBalance,
            @JsonProperty("cumRealisedPnl") BigDecimal cumRealisedPnl,
            @JsonProperty("locked") BigDecimal locked,
            @JsonProperty("marginCollateral") boolean marginCollateral,
            @JsonProperty("coin") String coin
    ) {}

    public record RetExtInfo(
            // Пустой объект в вашем примере
    ) {}
}
