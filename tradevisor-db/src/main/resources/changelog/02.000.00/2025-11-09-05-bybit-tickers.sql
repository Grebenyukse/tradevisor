--liquibase formatted sql

--changeset SE-Grebenyuk:02.000.00/TVR-5/load-init-data-bybit-quotes-providers
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM tradevisor.quotes_providers where "name" = 'bybit'
INSERT INTO tradevisor.quotes_providers ("name") VALUES
	 ('bybit'),
	 ('bybit-failed');

