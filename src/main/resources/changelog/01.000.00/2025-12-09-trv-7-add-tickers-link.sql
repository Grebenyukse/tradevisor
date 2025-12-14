--liquibase formatted sql

--changeset se-grebenyuk:01.000.00/trv-7/add-tickers-link
--rollback alter table tradevisor.tickers drop column if exists trade_ticker_code;
alter table tradevisor.tickers
    add column if not exists trade_ticker_code varchar
    references tradevisor.tickers (ticker_code);

create index if not exists idx_tickers_trade_ticker_code
    on tradevisor.tickers (trade_ticker_code);