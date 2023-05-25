--liquibase formatted sql

--changeset SE-Grebenyuk:01.000.00/TVR-1/init-schema
--rollback drop table if exists tradevisor.stocks;
create table if not exists tradevisor.stocks
(
    figi        varchar primary key,
    market_type varchar,
    body        jsonb,
    active      bool null
);
