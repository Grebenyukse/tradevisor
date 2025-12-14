package ru.grnk.tradevisor.integration.bybit.dto;

import java.math.BigDecimal;
import java.util.List;

public record BybitWalletBalanceResponse(
        int retCode,
        String retMsg,
        WalletResult result
) {
    public record WalletResult(
            List<Wallet> list
    ) {
    }

    public record Wallet(
            String coin,
            BigDecimal walletBalance,
            BigDecimal availableToWithdraw
    ) {
    }
}