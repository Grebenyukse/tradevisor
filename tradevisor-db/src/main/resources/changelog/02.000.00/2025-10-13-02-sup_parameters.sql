--liquibase formatted sql

--changeset SE-Grebenyuk:02.000.00/TVR-1/load-init-data
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM tradevisor.sup_parameters
INSERT INTO tradevisor.sup_parameters (property_name,property_value) VALUES
	 ('shares_ticker_names_loaded','true');
