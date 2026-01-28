#!/bin/bash

# Secure deployment script for TradeVisor
# This script deploys the application without storing secrets in .env files

set -e  # Exit on any error

# Function to check if required environment variables are set
check_required_vars() {
    local required_vars=(
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
        "EVENTS_TOPIC_THREAD_ID"
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
        "SPRING_DATASOURCE_URL"
        "SPRING_DATASOURCE_USERNAME"
        "SPRING_DATASOURCE_PASSWORD"
    )

    local missing_vars=()
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
}

# Check required variables
check_required_vars

# Create necessary directories
mkdir -p /home/"$(whoami)"/app/infra/nginx/certs
mkdir -p /home/"$(whoami)"/app/infra/data/certbot/conf
mkdir -p /home/"$(whoami)"/app/infra/data/certbot/www

# Navigate to the correct directory
cd /home/"$(whoami)"/app/infra

# Stop existing containers
echo "Stopping existing containers..."
docker compose down || true

# Kill processes on ports 80, 443 and 5432
echo "Freeing up ports..."
sudo fuser -k 80/tcp || true
sudo fuser -k 443/tcp || true
sudo fuser -k 5432/tcp || true

# Start containers with environment variables passed directly
echo "Starting containers with secure environment variable passing..."
# In your deploy-secure.sh, replace the docker compose up line with:
echo "Starting containers with secure environment variable passing..."

# Pass all variables explicitly to docker compose
docker compose \
  -e CHAT_ID="$CHAT_ID" \
  -e CHAT_TOKEN="$CHAT_TOKEN" \
  -e BASE_URL="$BASE_URL" \
  -e SUPERGROUP_CHAT_ID="$SUPERGROUP_CHAT_ID" \
  -e POSITIONS_TOPIC_THREAD_ID="$POSITIONS_TOPIC_THREAD_ID" \
  -e ORDERS_TOPIC_THREAD_ID="$ORDERS_TOPIC_THREAD_ID" \
  -e RUS_TOPIC_THREAD_ID="$RUS_TOPIC_THREAD_ID" \
  -e WORLD_TOPIC_THREAD_ID="$WORLD_TOPIC_THREAD_ID" \
  -e CRYPTO_TOPIC_THREAD_ID="$CRYPTO_TOPIC_THREAD_ID" \
  -e LOGS_TOPIC_THREAD_ID="$LOGS_TOPIC_THREAD_ID" \
  -e STATISTICS_TOPIC_THREAD_ID="$STATISTICS_TOPIC_THREAD_ID" \
  -e ERRORS_TOPIC_THREAD_ID="$ERRORS_TOPIC_THREAD_ID" \
  -e EVENTS_TOPIC_THREAD_ID="$EVENTS_TOPIC_THREAD_ID" \
  -e TINKOFF_INVEST_TOKEN="$TINKOFF_INVEST_TOKEN" \
  -e FINAM_API_KEY="$FINAM_API_KEY" \
  -e FINAM_ACCOUNT_KEY="$FINAM_ACCOUNT_KEY" \
  -e BYBIT_KEY="$BYBIT_KEY" \
  -e BYBIT_SECRET="$BYBIT_SECRET" \
  -e CLOUDRU_API_KEY="$CLOUDRU_API_KEY" \
  -e PROXY_API_KEY="$PROXY_API_KEY" \
  -e DEEPSEEK_KEY="$DEEPSEEK_KEY" \
  -e GIGACHAT_CLIENT_ID="$GIGACHAT_CLIENT_ID" \
  -e GIGACHAT_CLIENT_SECRET="$GIGACHAT_CLIENT_SECRET" \
  -e NEWS_API_KEY="$NEWS_API_KEY" \
  -e SPRING_DATASOURCE_URL="$SPRING_DATASOURCE_URL" \
  -e SPRING_DATASOURCE_USERNAME="$SPRING_DATASOURCE_USERNAME" \
  -e SPRING_DATASOURCE_PASSWORD="$SPRING_DATASOURCE_PASSWORD" \
  up -d

echo "Deployment completed successfully!"
echo "Note: No .env files were created. All secrets are passed directly to Docker containers."

# Show running containers
docker ps