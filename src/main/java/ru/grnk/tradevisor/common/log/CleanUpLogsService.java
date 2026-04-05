package ru.grnk.tradevisor.common.log;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class CleanUpLogsService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void deleteByLogDateBefore(LocalDateTime cutoffDate) {
        String sql = "DELETE FROM tradevisor.log_table WHERE log_date < ?";
        jdbcTemplate.update(sql, cutoffDate);
    }
}
