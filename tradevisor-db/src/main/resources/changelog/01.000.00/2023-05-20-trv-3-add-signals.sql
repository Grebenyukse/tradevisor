--liquibase formatted sql

--changeset SE-Grebenyuk:01.000.00/TVR-1/init-schema
--rollback drop table if exists tradevisor.signals;
create table if not exists tradevisor.signals
(
    id          serial primary key,
    name        varchar,
    description varchar,
    instrument_uuid      varchar references tradevisor.tickers (uuid),
    direction   smallint, -- 1 long -1 short
    price_open  real,
    stop_loss   real,
    take_profit real,
    created_at  timestamptz,
    updated_at  timestamptz,
    status      varchar --created, expired, executed, cancelled, published
);

alter table tradevisor.signals add constraint signal_on_bar_calculated_once_per_strategy unique
    (name, instrument_uuid, created_at, direction);
