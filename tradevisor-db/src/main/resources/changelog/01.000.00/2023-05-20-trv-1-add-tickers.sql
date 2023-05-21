--liquibase formatted sql

--changeset SE-Grebenyuk:01.000.00/TVR-1/add-tickers
--rollback drop table if exists tradevisor.tickers;
create table if not exists tradevisor.tickers
(
    ticker      varchar primary key,
    description varchar,
    market_type varchar,
    precision   int2,
    lot         int2,
    go          int2,
    expiration  timestamp
);
