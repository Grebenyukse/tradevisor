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
select id, name, SPLIT_PART(ticker_code, '@', 1), status, direction, price_open, stop_loss, take_profit from tradevisor.signals s ;
