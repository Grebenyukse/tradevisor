--liquibase formatted sql

--changeset SE-Grebenyuk:01.000.00/TVR-0/init-schema
--rollback drop schema if exists tradevisor cascade;
CREATE SCHEMA IF NOT EXISTS tradevisor;