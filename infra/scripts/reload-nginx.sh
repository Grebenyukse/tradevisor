#!/bin/sh
echo "🔄 reinitializing configuration..."

# Остановить Nginx
if pgrep nginx > /dev/null; then
  nginx -s quit 2>/dev/null || pkill nginx
  sleep 2
fi

# Переинициализировать конфигурацию
/usr/local/bin/init-ssl.sh
