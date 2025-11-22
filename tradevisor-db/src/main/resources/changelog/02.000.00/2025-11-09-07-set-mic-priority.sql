--liquibase formatted sql

--changeset SE-Grebenyuk:02.000.00/TVR-7/set-ticker-load-priority
update tradevisor.tickers set load_priority = 400 where provider = 'yahoofinance';
update tradevisor.tickers set load_priority = 300 where provider = 'bybit';
update tradevisor.tickers set load_priority = 200 where provider = 'tinkoff';
update tradevisor.tickers set load_priority = 100 where provider = 'fianm' and exchange = 'MISX';
update tradevisor.tickers set load_priority = 90 where provider = 'fianm' and exchange = 'RUSX';
update tradevisor.tickers set load_priority = 82 where provider = 'fianm' and exchange = '_SPBZ';
update tradevisor.tickers set load_priority = 81 where provider = 'fianm' and exchange = '_MMBZ';
update tradevisor.tickers set load_priority = 80 where provider = 'fianm' and exchange = 'RTSX';
update tradevisor.tickers set load_priority = 70 where provider = 'fianm' and exchange = 'bybit';
update tradevisor.tickers set load_priority = 60 where provider = 'fianm' and exchange = '_CRYP';
update tradevisor.tickers set load_priority = 50 where provider = 'fianm' and exchange = '_NPRO';
update tradevisor.tickers set load_priority = 40 where provider = 'fianm' and exchange = 'XNYS';



