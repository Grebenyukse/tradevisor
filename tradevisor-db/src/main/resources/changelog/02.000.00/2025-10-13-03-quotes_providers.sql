--liquibase formatted sql

--changeset SE-Grebenyuk:02.000.00/TVR-1/load-init-data
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM tradevisor.quotes_providers
INSERT INTO tradevisor.quotes_providers ("name") VALUES
	 ('finam'),
	 ('tinkoff'),
	 ('yahoofinance');
