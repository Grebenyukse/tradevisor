--liquibase formatted sql

--changeset se-grebenyuk:01.000.00/trv-7/init-finam-tables
--rollback drop table if exists tradevisor.finam_exchanges;
create table if not exists tradevisor.finam_exchanges(
    id serial primary key,
    name varchar(100),
    mic varchar(100),
    is_active bool
);

--rollback drop table if exists tradevisor.finam_tickers;
create table if not exists tradevisor.finam_tickers(
    id varchar not null primary key,
    ticker varchar not null,
    mic varchar references tradevisor.finam_exchanges(mic),
    isin varchar not null,
    name varchar,
    type varchar,
    t_uuid varchar references tradevisor.tickers(uuid)
);