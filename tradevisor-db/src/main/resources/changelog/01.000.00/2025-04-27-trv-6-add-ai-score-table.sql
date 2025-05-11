--liquibase formatted sql

--changeset se-grebenyuk:01.000.00/tvr-1/init-schema
--rollback drop table if exists tradevisor.ai_score;
create table if not exists tradevisor.ai_score (
    request_date date not null,
    ticker varchar(20) not null,
    score int not null,
    source varchar(20) not null,
    unique (request_date, ticker, source)
);