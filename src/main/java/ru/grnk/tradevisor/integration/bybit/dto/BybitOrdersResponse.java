package ru.grnk.tradevisor.integration.bybit.dto;

import java.util.List;

public record BybitOrdersResponse(
        int retCode,
        String retMsg,
        OrderResult result
) {
    public record OrderResult(
            List<OrderInfo> list
    ) {
    }

    public record OrderInfo(
            String orderId,
            String symbol,
            String side,
            String orderType,
            String qty,
            String price,
            String orderStatus
    ) {
    }
}