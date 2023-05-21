--liquibase formatted sql

--changeset SE-Grebenyuk:01.000.00/TVR-1/add-tickers
--rollback drop table if exists tradevisor.tickers;
create table if not exists tradevisor.tickers
(
    uuid        varchar not null primary key,
    ticker      varchar not null,
    figi        varchar not null,
    description varchar,
    market_type varchar,
    exchange    varchar,
    precision   int4,
    lot         int4,
    go          int4,
    expiration  timestamp,
    currency    varchar
);
