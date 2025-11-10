--liquibase formatted sql

--changeset SE-Grebenyuk:01.000.00/TVR-8/add-tickers-status
--rollback alter table tradevisor.tickers drop column if exists status;
ALTER TABLE tradevisor.tickers
ADD COLUMN IF NOT EXISTS status VARCHAR(100);
