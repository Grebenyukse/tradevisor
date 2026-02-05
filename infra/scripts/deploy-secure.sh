#!/bin/bash

# Secure deployment script for TradeVisor
set -e

# Check if required environment variables are set
required_vars=(
    "CHAT_ID"
    "CHAT_TOKEN"
    "BASE_URL"
    "SUPERGROUP_CHAT_ID"
    "POSITIONS_TOPIC_THREAD_ID"
    "ORDERS_TOPIC_THREAD_ID"
    "RUS_TOPIC_THREAD_ID"
    "WORLD_TOPIC_THREAD_ID"
    "CRYPTO_TOPIC_THREAD_ID"
    "LOGS_TOPIC_THREAD_ID"
    "STATISTICS_TOPIC_THREAD_ID"
    "ERRORS_TOPIC_THREAD_ID"
    "EVENTS_THREAD_ID"
    "TINKOFF_INVEST_TOKEN"
    "FINAM_API_KEY"
    "FINAM_ACCOUNT_KEY"
    "BYBIT_KEY"
    "BYBIT_SECRET"
    "CLOUDRU_API_KEY"
    "PROXY_API_KEY"
    "DEEPSEEK_KEY"
    "GIGACHAT_CLIENT_ID"
    "GIGACHAT_CLIENT_SECRET"
    "NEWS_API_KEY"
    "SPRING_DATASOURCE_PASSWORD_V2"
)

missing_vars=()
for var in "${required_vars[@]}"; do
    if [[ -z "${!var}" ]]; then
        missing_vars+=("$var")
    fi
done

if [[ ${#missing_vars[@]} -gt 0 ]]; then
    echo "Error: The following required environment variables are not set:"
    printf '%s\n' "${missing_vars[@]}"
    echo "Please export these variables before running this script."
    exit 1
fi

# Create necessary directories
mkdir -p /home/"$(whoami)"/app/infra/nginx/certs
mkdir -p /home/"$(whoami)"/app/infra/data/certbot/conf
mkdir -p /home/"$(whoami)"/app/infra/data/certbot/www

cd /home/"$(whoami)"/app/infra

# Stop existing containers
echo "Stopping existing containers..."
docker compose down || true

# Kill processes on ports 80, 443 and 5432
echo "Freeing up ports..."
sudo fuser -k 80/tcp || true
sudo fuser -k 443/tcp || true
sudo fuser -k 5432/tcp || true

# Start containers - Docker Compose will automatically pick up environment variables
echo "Starting containers with environment variables..."
docker compose up -d

echo "Deployment completed successfully!"

# Show running containers
docker ps
