# TradeVisor Deployment Guide

This document describes how to deploy the TradeVisor application to a Ubuntu 22.04 server with the following specifications:
- 4GB RAM
- 2 CPU cores
- 30GB disk space

## Prerequisites

1. Ubuntu 22.04 server with SSH access
2. Domain name pointing to the server (for SSL)
3. GitHub repository secrets configured for deployment

## Server Preparation

### 1. Install Docker

Run the provided installation script on your server:

```bash
chmod +x infra/scripts/install-docker.sh
./infra/scripts/install-docker.sh
```

After installation, log out and log back in for the group changes to take effect.

### 2. Verify Docker Installation

```bash
docker --version
docker compose version
docker run hello-world
```

## Application Deployment

### Option 1: Manual Deployment

1. Build the application JAR file:
   ```bash
   ./gradlew build -x test
   ```

2. Copy the application files to your server:
   ```bash
   scp build/libs/*.jar user@your-server:/home/user/tradevisor/app.jar
   scp -r infra/ user@your-server:/home/user/tradevisor/
   ```

3. Set environment variables directly (recommended for security):
   ```bash
   # Export all required environment variables
   export CHAT_ID=your_chat_id
   export CHAT_TOKEN=your_bot_token
   export BASE_URL=https://yourdomain.com
   # ... (export all other required variables)
   
   # Then run the secure deployment script
   ./scripts/deploy-secure.sh
   ```

4. Navigate to the infra directory:
   ```bash
   cd /home/user/tradevisor/infra
   ```

5. Start the services:
   ```bash
   docker compose up -d
   ```

### Option 2: Secure Manual Deployment (Recommended)

For enhanced security, you can deploy without creating any `.env` files on the server:

1. Build the application JAR file:
   ```bash
   ./gradlew build -x test
   ```
   
2. Copy the application files to your server:
   ```bash
   scp build/libs/*.jar user@your-server:/home/user/tradevisor/app.jar
   scp -r infra/ user@your-server:/home/user/tradevisor/
   ```
   
3. SSH into your server and set all environment variables:
   ```bash
   ssh user@your-server
   cd /home/user/tradevisor/infra
   
   # Export all required environment variables
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
   export LIQUIBASE_URL=jdbc:postgresql://postgres:5432/postgres
   export LIQUIBASE_USERNAME=postgres
   export LIQUIBASE_PASSWORD=password
   ```
   
4. Run the secure deployment script:
   ```bash
   chmod +x ./scripts/deploy-secure.sh
   ./scripts/deploy-secure.sh
   ```

This approach ensures that no sensitive data is stored in files on the server filesystem.

### Option 3: GitHub Actions Deployment

1. Set up the following secrets in your GitHub repository:
   - `SSH_PRIVATE_KEY`: Private SSH key for accessing the server
   - `SERVER_USER`: Username for SSH access
   - `SERVER_IP`: IP address of your server
   - All the required environment variables as individual secrets:
     - `CHAT_ID`, `CHAT_TOKEN`, `BASE_URL`, `SUPERGROUP_CHAT_ID`, `POSITIONS_TOPIC_THREAD_ID`,
       `ORDERS_TOPIC_THREAD_ID`, `RUS_TOPIC_THREAD_ID`, `WORLD_TOPIC_THREAD_ID`, `CRYPTO_TOPIC_THREAD_ID`,
       `LOGS_TOPIC_THREAD_ID`, `STATISTICS_TOPIC_THREAD_ID`, `ERRORS_TOPIC_THREAD_ID`, `EVENTS_TOPIC_THREAD_ID`,
       `TINKOFF_INVEST_TOKEN`, `FINAM_API_KEY`, `FINAM_ACCOUNT_KEY`, `BYBIT_KEY`, `BYBIT_SECRET`,
       `CLOUDRU_API_KEY`, `PROXY_API_KEY`, `DEEPSEEK_KEY`, `GIGACHAT_CLIENT_ID`, `GIGACHAT_CLIENT_SECRET`,
       `NEWS_API_KEY`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`,
       `LIQUIBASE_URL`, `LIQUIBASE_USERNAME`, `LIQUIBASE_PASSWORD`

2. Push changes to the main branch to trigger the deployment workflow.

The workflow will:
- Build the application JAR file
- Copy the JAR file to the server
- Copy infrastructure files to the server
- Pass environment variables directly to Docker containers (without creating .env files)
- Start the containers using Docker Compose with secure environment variable handling

This approach ensures that no sensitive data is stored in files on the server filesystem. All secrets are passed directly to the Docker containers through environment variables.

## Resource Allocation

Containers are configured with the following resource limits to fit within the 4GB RAM and 2 CPU constraints:

- **PostgreSQL**: 1GB RAM, 0.5 CPU
- **TradeVisor App**: 2GB RAM, 1.0 CPU
- **Nginx**: 512MB RAM, 0.5 CPU
- **Prometheus**: (existing config)
- **Grafana**: (existing config)

## SSL Configuration

To enable SSL, you need to provide your certificates:

1. Place your SSL certificate and key in `infra/nginx/certs/`:
   - Certificate: `cert.pem`
   - Private key: `key.pem`

2. Update the domain name in `infra/nginx/conf.d/tradevisor.conf` to match your domain.

## Environment Variables

The application requires several environment variables for integrations. For security reasons, these should be passed directly to Docker containers rather than stored in files:

```bash
# Export all required environment variables
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
export LIQUIBASE_URL=jdbc:postgresql://postgres:5432/postgres
export LIQUIBASE_USERNAME=postgres
export LIQUIBASE_PASSWORD=password
```

Then run the application with:
```bash
docker compose up -d
```

This approach ensures that no sensitive data is stored in files on the server filesystem.

## Monitoring

The deployment includes Prometheus and Grafana for monitoring:

- Prometheus: http://your-server:9090
- Grafana: http://your-server:3000

## Maintenance

### View Logs

```bash
# View all container logs
docker compose logs -f

# View specific service logs
docker compose logs -f tradevisor
```

### Update Application

```bash
# Pull latest changes
git pull

# Rebuild and restart containers
docker compose up -d --build
```

### Backup Database

```bash
# Create database backup
docker exec postgres pg_dump -U postgres postgres > backup.sql