--liquibase formatted sql

--changeset SE-Grebenyuk:01.000.00/TVR-1/add-tickers
--rollback drop table if exists tradevisor.quotes_providers;
create table if not exists tradevisor.quotes_providers(
name varchar not null primary key
);

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
    provider varchar null references tradevisor.quotes_providers(name)
);
