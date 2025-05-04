package ru.grnk.tradevisor.acollect.aiscore.dto;

public record AiScoreEntity(
        java.sql.Date requestDate,
        Integer score,
        String ticker,
        String source

) {

}
