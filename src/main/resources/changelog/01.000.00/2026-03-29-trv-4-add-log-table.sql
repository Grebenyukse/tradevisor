--liquibase formatted sql

--changeset SE-Grebenyuk:01.000.00/TVR-4/init-log-table
--rollback drop table if exists tradevisor.log_table;
CREATE TABLE IF NOT EXISTS tradevisor.log_table (
    id BIGSERIAL PRIMARY KEY,
    log_level VARCHAR(10) NOT NULL,
    log_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    message TEXT,
    logger_name VARCHAR(255),
    thread_name VARCHAR(255),
    stack_trace TEXT
);

CREATE INDEX IF NOT EXISTS idx_log_table_log_date ON tradevisor.log_table(log_date);
CREATE INDEX IF NOT EXISTS idx_log_table_log_level ON tradevisor.log_table(log_level);