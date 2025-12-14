--liquibase formatted sql

--changeset se-grebenyuk:01.000.00/tvr-1/add-tickers-table
--rollback drop table if exists tradevisor.tickers;
create table if not exists tradevisor.tickers(
    ticker_code        varchar not null primary key, -- ticker@mic
    ticker      varchar not null,
    figi        varchar not null,
    description varchar,
    market_type varchar,
    exchange    varchar,
    precision   int4,
    lot         int4,
    go          int4,
    expiration  timestamp,
    currency    varchar,
    provider varchar null,
    status varchar(100),
    load_priority int4 default 1
);
