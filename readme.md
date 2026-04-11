## сервер
ssh tradevisor@45.155.205.225
./app/infra/scripts/ttools.sh - установить инструменты командной строки

# скачать логи с сервера
cd ~/IdeaProjects/tradevisor/logs/
./getLogs.sh

## запуск telegram bot локально:
1. cd C:\Users\grebe\Downloads\clo-2.4.5-stable-windows-x86_64
2.  .\clo.exe publish http 8080
## антивирус
sudo chkrootkit

# просмотр сигналов в бд
select s.id, s.name, SPLIT_PART(s.ticker_code, '@', 1), SPLIT_PART(s.ticker_code, '@', 2) exchange, t.ticker as ticker,  
s.status, s.direction, s.price_open, s.stop_loss, s.take_profit, s.risk_lot, s.tp_ticks, s.sl_ticks, s.tp_2_sl_ratio
from tradevisor.signals s
join tradevisor.tickers t on s.ticker_code = t.ticker_code
order by exchange, s.name, s.id ;
