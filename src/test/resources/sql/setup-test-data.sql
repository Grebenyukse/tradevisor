delete from tradevisor.tickers where 1=1;
delete from tradevisor.market_data where 1=1;
INSERT INTO tradevisor.tickers (ticker_code, ticker, exchange, spot_ticker_code, description, currency, provider, status, "version")
VALUES('TEST_TICKER@bybit', 'TEST_TICKER', 'bybit', null , 'my spot', 'RUB', 'bybit', null , 0);
INSERT INTO tradevisor.tickers (ticker_code, ticker, exchange, spot_ticker_code, description, currency, provider, status, "version")
VALUES('TEST_TICKER@finam', 'TEST_TICKER', 'finam', null , 'my spot', 'RUB', 'finam', null , 0);
INSERT INTO tradevisor.tickers (ticker_code, ticker, exchange, spot_ticker_code, description, currency, provider, status, "version")
VALUES('TEST_TICKER@yahoofinance', 'TEST_TICKER', 'yahoofinance', null , 'my spot', 'USD', 'yahoofinance', null , 0);
INSERT INTO tradevisor.tickers (ticker_code, ticker, exchange, spot_ticker_code, description, currency, provider, status, "version")
VALUES('TEST_TICKER@tinkoff', 'TEST_TICKER', 'tinkoff', null , 'my spot', 'RUB', 'tinkoff', null , 0);