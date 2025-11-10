--liquibase formatted sql

--changeset SE-Grebenyuk:01.000.00/TVR-8/add-tickers-load-priority
--rollback alter table tradevisor.tickers drop column if exists load_priority;
ALTER TABLE tradevisor.tickers ADD COLUMN IF NOT EXISTS load_priority int4 default 1;
