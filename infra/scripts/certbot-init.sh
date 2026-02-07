#!/bin/sh

echo "🚀 Starting Certbot initialization..."

# Создать cron job для автоматического обновления сертификатов
echo "0 12 * * * certbot renew --quiet >> /var/log/certbot-renew.log 2>&1" | crontab -

# Запустить начальное получение/обновление сертификатов
echo "🔑 Running initial certificate request/renewal..."
certbot certonly --webroot --webroot-path=/var/www/certbot --email bugor-p@yandex.ru --agree-tos --no-eff-email -d grebenyukse.ru -d www.grebenyukse.ru --non-interactive || echo "⚠️  Initial certificate request failed or certificates already exist"

# Запустить cron daemon для периодических обновлений
echo "⏰ Starting cron daemon for automatic renewal..."
crond

echo "✅ Certbot service initialized successfully"
echo "📋 Cron job scheduled for daily certificate renewal at 12:00"

# Держим контейнер активным
tail -f /dev/null
