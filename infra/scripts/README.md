# TradeVisor Deployment Scripts

This directory contains scripts for deploying the TradeVisor application securely.

## Secure Deployment

For enhanced security, we provide `deploy-secure.sh` which deploys the application without storing secrets in `.env` files on the server filesystem.

### How it works

1. Environment variables are passed directly to Docker containers via the `docker compose` command
2. No `.env` files are created on the server
3. All secrets are loaded into memory only during the deployment process

### Usage

1. Export all required environment variables:
   ```bash
   export CHAT_ID=your_chat_id
   export CHAT_TOKEN=your_bot_token
   export BASE_URL=https://yourdomain.com
   export SUPERGROUP_CHAT_ID=your_supergroup_id
   export POSITIONS_TOPIC_THREAD_ID=positions_thread_id
   export ORDERS_TOPIC_THREAD_ID=orders_thread_id
   export RUS_TOPIC_THREAD_ID=rus_thread_id
   export WORLD_TOPIC_THREAD_ID=world_thread_id
   export CRYPTO_TOPIC_THREAD_ID=crypto_thread_id
   export LOGS_TOPIC_THREAD_ID=logs_thread_id
   export STATISTICS_TOPIC_THREAD_ID=statistics_thread_id
   export ERRORS_TOPIC_THREAD_ID=errors_thread_id
   export EVENTS_TOPIC_THREAD_ID=events_thread_id
   export TINKOFF_INVEST_TOKEN=your_tinkoff_token
   export FINAM_API_KEY=your_finam_api_key
   export FINAM_ACCOUNT_KEY=your_finam_account_id
   export BYBIT_KEY=your_bybit_key
   export BYBIT_SECRET=your_bybit_secret
   export CLOUDRU_API_KEY=your_cloudru_key
   export PROXY_API_KEY=your_proxy_key
   export DEEPSEEK_KEY=your_deepseek_key
   export GIGACHAT_CLIENT_ID=your_gigachat_client_id
   export GIGACHAT_CLIENT_SECRET=your_gigachat_client_secret
   export NEWS_API_KEY=your_news_api_key
   # Database configuration
   export SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/postgres
   export SPRING_DATASOURCE_USERNAME=postgres
   export SPRING_DATASOURCE_PASSWORD=password
   ```

2. Run the secure deployment script:
   ```bash
   chmod +x deploy-secure.sh
   ./deploy-secure.sh
   ```

This approach ensures that no sensitive data is stored in files on the server filesystem, reducing the risk of accidental exposure.