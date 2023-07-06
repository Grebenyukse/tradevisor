--liquibase formatted sql

--changeset SE-Grebenyuk:01.000.00/TVR-1/init-schema
--rollback drop table if exists tradevisor.sup_parameters;
create table if not exists tradevisor.sup_parameters
(
    property_name  varchar,
    property_value varchar
);


insert into tradevisor.sup_parameters (property_name, property_value)
VALUES ('shares_ticker_names_loaded', 'false');