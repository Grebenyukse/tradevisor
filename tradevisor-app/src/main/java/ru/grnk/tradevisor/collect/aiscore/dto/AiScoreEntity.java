package ru.grnk.tradevisor.collect.aiscore.dto;

public record AiScoreEntity(
        java.sql.Date requestDate,
        Integer score,
        String ticker,
        String source

) {

}
