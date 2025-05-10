package ru.grnk.tradevisor.acollect.forecast.aiscore;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.acollect.forecast.aiscore.dto.AiScoreEntity;
import ru.grnk.tradevisor.acollect.forecast.aiscore.dto.AiScoreResponse;

import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class AiScoreRepository {

    private final JdbcTemplate jdbcTemplate;
    private static final String sql = """
                        INSERT INTO tradevisor.ai_score (request_date, ticker, score, source)
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT (request_date, ticker, source) DO UPDATE SET
                            ticker = EXCLUDED.ticker,
                            score = EXCLUDED.score,
                            source = EXCLUDED.source""";


    public void saveAiScore(AiScoreResponse aiScoreResponse, String source) {
        jdbcTemplate.update(
                sql,
                Date.from(Instant.now()),
                aiScoreResponse.ticker(),
                aiScoreResponse.score(),
                source
        );
    }

    public void saveAiScore(List<AiScoreEntity> aiScoreResponses) {
        if (aiScoreResponses == null || aiScoreResponses.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                sql,
                aiScoreResponses.stream()
                        .map(response -> new Object[]{
                                response.requestDate(),
                                response.ticker(),
                                response.score(),
                                response.source()
                        })
                        .collect(Collectors.toList())
        );
    }
}
