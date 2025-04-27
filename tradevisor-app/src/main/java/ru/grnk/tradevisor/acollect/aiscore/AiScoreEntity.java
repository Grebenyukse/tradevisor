package ru.grnk.tradevisor.acollect.aiscore;

public record AiScoreEntity(
        java.sql.Date requestDate,
        Integer score,
        String ticker,
        String source

) {

}
