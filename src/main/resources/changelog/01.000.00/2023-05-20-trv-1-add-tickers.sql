--liquibase formatted sql

--changeset se-grebenyuk:01.000.00/tvr-1/add-tickers-table
--rollback drop table if exists tradevisor.tickers;
create table if not exists tradevisor.tickers(
    ticker_code     varchar not null primary key, -- ticker@mic / uid
    ticker          varchar not null,
    exchange        varchar,
    spot_ticker_code varchar,
    description     varchar,
    currency        varchar,
    provider        varchar null,
    status          varchar(100),
    version         int4 default 0
);

-- Исправленное создание внешнего ключа
alter table tradevisor.tickers
    add constraint fk_spot_ticker_code_2_ticker_code
    foreign key (spot_ticker_code) references tradevisor.tickers (ticker_code);

-- Создание индекса для улучшения производительности
create index if not exists idx_tickers_spot_ticker_code
    on tradevisor.tickers (spot_ticker_code);
