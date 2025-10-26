INSERT INTO tradevisor.finam_exchanges ("name",mic,is_active) VALUES ('TEST_EXCHANGE','TEST_EXCHANGE', null);
INSERT INTO tradevisor.tickers
 (ticker_code,ticker,figi,description,market_type,exchange,
"precision",lot,"go",expiration,currency,provider)
 VALUES
('TEST_TICKER@TEST_EXCHANGE','TEST_TICKER','TEST_TICKER@TEST_EXCHANGE',
'test descrption','EQUITIES','TEST_EXCHANGE',NULL,NULL,NULL,NULL,'rub','finam');