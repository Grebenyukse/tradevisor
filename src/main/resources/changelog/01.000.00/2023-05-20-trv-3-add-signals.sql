--liquibase formatted sql

--changeset SE-Grebenyuk:01.000.00/TVR-1/init-schema
--rollback drop table if exists tradevisor.signals;
create table if not exists tradevisor.signals
(
    id          serial primary key,
    name        varchar,
    description varchar,
    ticker_code varchar references tradevisor.tickers (ticker_code),
    direction   smallint, -- 1 long -1 short
    price_open  real,
    stop_loss   real,
    take_profit real,
    created_at  timestamptz,
    updated_at  timestamptz,
    status      varchar, --created, expired, executed, cancelled, published, manual
    strategy_props jsonb -- { name: fibo, from_price: .., to_price:.., from_date:.., to_date:.., price618:..,price50:..,price318:.. }
                         -- { name: gap, from_price: .., to_price:.., from_date:.., to_date:.. }
);
