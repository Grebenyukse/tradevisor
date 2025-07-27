package ru.grnk.tradevisor.integration.finam.dto;

public record FinamGetTradesRs(
        List<AccountTrade> accountTrade
) {
}
