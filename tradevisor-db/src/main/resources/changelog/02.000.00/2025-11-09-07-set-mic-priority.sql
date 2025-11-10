--liquibase formatted sql

--changeset SE-Grebenyuk:02.000.00/TVR-7/set-ticker-load-priority
update tradevisor.tickers set load_priority = 100 where exchange = 'MISX';
update tradevisor.tickers set load_priority = 90 where exchange = 'RUSX';
update tradevisor.tickers set load_priority = 82 where exchange = '_SPBZ';
update tradevisor.tickers set load_priority = 81 where exchange = '_MMBZ';
update tradevisor.tickers set load_priority = 80 where exchange = 'RTSX';
update tradevisor.tickers set load_priority = 70 where exchange = 'bybit';
update tradevisor.tickers set load_priority = 60 where exchange = '_CRYP';
update tradevisor.tickers set load_priority = 50 where exchange = '_NPRO';
update tradevisor.tickers set load_priority = 40 where exchange = 'XNYS';



