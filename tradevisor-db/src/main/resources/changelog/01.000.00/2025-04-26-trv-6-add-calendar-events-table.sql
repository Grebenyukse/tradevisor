--liquibase formatted sql

--changeset se-grebenyuk:01.000.00/tvr-1/init-schema
--rollback drop index if exists idx_economic_events_category;
--rollback drop index if exists idx_economic_events_date;
--rollback drop table if exists tradevisor.economic_events;
create table if not exists tradevisor.economic_events (
    event_id varchar(32) primary key,
    title text not null,
    event_date timestamp with time zone not null,
    category varchar(20) not null check (category in (
        'crypto', 'stock', 'etf', 'forex',
        'futures', 'ipo', 'economic'
    )),
    source varchar(50) not null,
    impact int,
    description text,
    country varchar(3),
    ticker_uid varchar(10),
    created_at timestamp with time zone default now()
);

create index idx_economic_events_category on tradevisor.economic_events (category);
create index idx_economic_events_date on tradevisor.economic_events (event_date);