#!/bin/sh

set -e

CERT_PATH="/etc/letsencrypt/live/grebenyukse.ru"
TEMPLATE_FILE="/etc/nginx/conf.d/tradevisor-https.conf.template"
HTTPS_CONFIG="/etc/nginx/conf.d/tradevisor-https.conf"
WEBROOT_PATH="/var/www/certbot"

echo "🚀 Starting SSL orchestration..."

# Убедиться, что пути существуют
mkdir -p "$WEBROOT_PATH"
mkdir -p "$(dirname "$CERT_PATH")"

# Функция запуска/перезапуска Nginx
start_nginx() {
  if pgrep nginx > /dev/null; then
    echo "🔁 Reloading Nginx..."
    nginx -s reload 2>/dev/null || nginx
  else
    echo "🟢 Starting Nginx..."
    nginx
  fi
}

# Функция инициализации конфигурации
initialize_config() {
  echo "🔍 Checking for SSL certificates..."
  rm -f "$HTTPS_CONFIG"

  if [ -f "$CERT_PATH/fullchain.pem" ] && [ -f "$CERT_PATH/privkey.pem" ]; then
    echo "✅ SSL certificates found, enabling HTTPS"
    # Включаем HTTPS конфиг через копирование template файла
    cp "$TEMPLATE_FILE" "$HTTPS_CONFIG"
    echo "🔒 HTTPS configuration enabled"
  else
    echo "🟡 No SSL certificates found, HTTP only mode"
  fi

  start_nginx
}

# Инициализация
initialize_config

# Установить inotify-tools
echo "📦 Installing inotify-tools..."
if command -v apk >/dev/null 2>&1; then
  apk add --no-cache inotify-tools > /dev/null 2>&1
elif command -v apt-get >/dev/null 2>&1; then
  apt-get update && apt-get install -y inotify-tools > /dev/null 2>&1
elif command -v yum >/dev/null 2>&1; then
  yum install -y inotify-tools > /dev/null 2>&1
fi

# Настроить cron job для мониторинга изменений сертификатов
echo "⏰ Setting up cron job for certificate monitoring..."

# Добавить cron job (каждую минуту) - предполагается, что check-certificates.sh уже скопирован в контейнер
echo "* * * * * /usr/local/bin/check-certificates.sh >> /var/log/certificate-monitor.log 2>&1" | crontab -

# Запустить cron daemon
echo "🟢 Starting cron daemon..."
crond

echo "✅ SSL orchestration initialized successfully"
echo "📋 Cron job scheduled to check certificates every minute"

# Держим контейнер активным
tail -f /dev/null
