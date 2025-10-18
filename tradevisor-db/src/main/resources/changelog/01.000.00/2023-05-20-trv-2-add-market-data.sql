--liquibase formatted sql

--changeset SE-Grebenyuk:01.000.00/TVR-1/init-schema
--rollback drop table tradevisor.market_data
create table if not exists tradevisor.market_data
(
    id     serial primary key,
    ticker_code  varchar   not null references tradevisor.tickers (ticker_code),
    open   real      null,
    high   real      null,
    low    real      null,
    close  real      null,
    time   timestamptz not null
);

create index if not exists ix_tradevisor_market_data_ticker on tradevisor.market_data (ticker_code, time);