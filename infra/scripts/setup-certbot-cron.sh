#!/bin/bash

PROJECT_DIR="/home/${USER}/app/infra"

# Проверяем, существует ли уже запись в crontab
(crontab -l 2>/dev/null | grep -q "$PROJECT_DIR") && { echo "Cron job already exists."; exit 0; }

# Добавляем задачу в crontab
(crontab -l 2>/dev/null; echo "0 12 * * * cd $PROJECT_DIR && docker compose run --rm certbot renew --quiet") | crontab -

echo "Cron job added successfully."
