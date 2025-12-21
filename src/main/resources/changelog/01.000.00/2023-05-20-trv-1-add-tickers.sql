--liquibase formatted sql

--changeset se-grebenyuk:01.000.00/tvr-1/add-tickers-table
--rollback drop table if exists tradevisor.tickers;
create table if not exists tradevisor.tickers(
    ticker_code     varchar not null primary key, -- ticker@mic / uid
    ticker          varchar not null,
    figi            varchar null,
    description     varchar,
    market_type     varchar,
    exchange        varchar,
    precision       int4,
    lot             int4,
    go              int4,
    expiration      timestamp,
    currency        varchar,
    provider        varchar null,
    status          varchar(100),
    load_priority   int4 default 1,
    version         int4 default 0
);

alter table tradevisor.tickers add column if not exists spot_ticker_code varchar references tradevisor.tickers (ticker_code);
create index if not exists idx_tickers_spot_ticker_code on tradevisor.tickers (spot_ticker_code);