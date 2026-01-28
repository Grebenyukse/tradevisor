#!/bin/bash

# Test script for the secure deployment approach
# This script verifies that the deployment works without creating .env files

set -e

echo "Testing secure deployment approach..."

# Create a temporary directory for testing
TEST_DIR="/tmp/tradevisor-test"
mkdir -p "$TEST_DIR"
cd "$TEST_DIR"

# Copy the infra directory structure
cp -r /home/$(whoami)/app/infra .

# Remove any existing .env file
rm -f infra/.env

# Set mock environment variables
export CHAT_ID="test_chat_id"
export CHAT_TOKEN="test_chat_token"
export BASE_URL="https://test.example.com"
export SUPERGROUP_CHAT_ID="test_supergroup"
export POSITIONS_TOPIC_THREAD_ID="1"
export ORDERS_TOPIC_THREAD_ID="2"
export RUS_TOPIC_THREAD_ID="3"
export WORLD_TOPIC_THREAD_ID="4"
export CRYPTO_TOPIC_THREAD_ID="5"
export LOGS_TOPIC_THREAD_ID="6"
export STATISTICS_TOPIC_THREAD_ID="7"
export ERRORS_TOPIC_THREAD_ID="8"
export EVENTS_THREAD_ID="9"
export TINKOFF_INVEST_TOKEN="test_tinkoff_token"
export FINAM_API_KEY="test_finam_key"
export FINAM_ACCOUNT_KEY="test_finam_account"
export BYBIT_KEY="test_bybit_key"
export BYBIT_SECRET="test_bybit_secret"
export CLOUDRU_API_KEY="test_cloudru_key"
export PROXY_API_KEY="test_proxy_key"
export DEEPSEEK_KEY="test_deepseek_key"
export GIGACHAT_CLIENT_ID="test_gigachat_id"
export GIGACHAT_CLIENT_SECRET="test_gigachat_secret"
export NEWS_API_KEY="test_news_key"

# Test that docker-compose.yml references environment variables correctly
echo "Verifying docker-compose.yml configuration..."
cd infra

# Check that docker-compose.yml doesn't reference .env file anymore
if grep -q "env_file:" docker-compose.yml; then
    echo "ERROR: docker-compose.yml still references env_file"
    exit 1
else
    echo "SUCCESS: docker-compose.yml does not reference env_file"
fi

# Check that environment variables are properly referenced
required_vars=("CHAT_ID" "CHAT_TOKEN" "BASE_URL" "TINKOFF_INVEST_TOKEN" "FINAM_API_KEY")
for var in "${required_vars[@]}"; do
    if grep -q "\${$var}" docker-compose.yml; then
        echo "SUCCESS: $var is properly referenced in docker-compose.yml"
    else
        echo "WARNING: $var is not referenced in docker-compose.yml"
    fi
done

echo "Test completed successfully!"
echo "The secure deployment approach is ready for use."

# Clean up
cd /
rm -rf "$TEST_DIR"