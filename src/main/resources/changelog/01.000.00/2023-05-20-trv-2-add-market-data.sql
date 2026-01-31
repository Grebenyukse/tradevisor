--liquibase formatted sql

--changeset SE-Grebenyuk:01.000.00/TVR-1/init-schema
--rollback drop table tradevisor.market_data
-- rollback drop table tradevisor.market_data
create table if not exists tradevisor.market_data
(
    ticker_code  varchar   not null references tradevisor.tickers (ticker_code),
    time   timestamptz not null,
    open   real      null,
    high   real      null,
    low    real      null,
    close  real      null
);
alter table tradevisor.market_data drop constraint if exists market_data_time_ticker_code_pk;
alter table tradevisor.market_data add constraint market_data_time_ticker_code_pk primary key (time, ticker_code);

