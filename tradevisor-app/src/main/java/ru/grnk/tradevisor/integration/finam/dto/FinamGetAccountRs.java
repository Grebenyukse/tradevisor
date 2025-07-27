package ru.grnk.tradevisor.integration.finam.dto;


import java.util.List;

public record FinamGetAccountRs(
        String account_id, // Идентификатор аккаунта
        String type,
        String status,
        String equity,
        String unrealized_profit,
        List<Position> positions,
        List<Money> cash
) {
}