--liquibase formatted sql

--changeset se-grebenyuk:01.000.00/trv-7/init-finam-tables
--rollback drop table if exists tradevisor.finam_exchanges;
create table if not exists tradevisor.finam_exchanges(
    id serial primary key,
    name varchar(100),
    mic varchar(100) unique,
    is_active bool
);