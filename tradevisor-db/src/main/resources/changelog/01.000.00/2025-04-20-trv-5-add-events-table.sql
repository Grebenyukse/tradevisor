--liquibase formatted sql

--changeset SE-Grebenyuk:01.000.00/TVR-1/init-schema
create table if not exists tradevisor.events(
    hash_id varchar not null primary key,
    event_date timestamp with time zone,
    category varchar,
    impact varchar,
    source  varchar,
    content varchar,
    instrument_uuid varchar not null references tradevisor.tickers (uuid)
);
CREATE INDEX if not exists idx_events_event_date_desc ON tradevisor.events (event_date DESC);
--rollback drop index if exists idx_events_event_date_desc;
--rollback drop table if exists tradevisor.events;