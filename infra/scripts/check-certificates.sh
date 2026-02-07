#!/bin/sh
CERT_PATH="/etc/letsencrypt/live/grebenyukse.ru"
LAST_CHECK="/tmp/last_cert_check"

# Получить время последней модификации сертификатов
if [ -f "$CERT_PATH/fullchain.pem" ] && [ -f "$CERT_PATH/privkey.pem" ]; then
    CURRENT_TIME=$(stat -c %Y "$CERT_PATH/fullchain.pem" 2>/dev/null || echo "0")
else
    CURRENT_TIME="0"
fi

# Прочитать время последней проверки
if [ -f "$LAST_CHECK" ]; then
    LAST_TIME=$(cat "$LAST_CHECK")
else
    LAST_TIME="0"
fi

# Если время изменилось - запустить перезагрузку
if [ "$CURRENT_TIME" != "$LAST_TIME" ]; then
    echo "$CURRENT_TIME" > "$LAST_CHECK"
    echo "🔔 Certificate change detected via cron"
    /usr/local/bin/reload-nginx.sh
fi
