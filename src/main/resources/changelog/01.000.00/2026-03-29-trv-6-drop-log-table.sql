--liquibase formatted sql

--changeset SE-Grebenyuk:01.000.00/TVR-6/drop-log-table
drop table if exists tradevisor.log_table;
