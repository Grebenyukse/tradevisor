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

1. Copy the application files to your server:
   ```bash
   scp -r infra/ user@your-server:/home/user/tradevisor/
   scp Dockerfile user@your-server:/home/user/tradevisor/
   ```

2. Navigate to the infra directory:
   ```bash
   cd /home/user/tradevisor/infra
   ```

3. Start the services:
   ```bash
   docker compose up -d
   ```

### Option 2: GitHub Actions Deployment

1. Set up the following secrets in your GitHub repository:
   - `SSH_PRIVATE_KEY`: Private SSH key for accessing the server
   - `SERVER_USER`: Username for SSH access
   - `SERVER_IP`: IP address of your server

2. Push changes to the main branch to trigger the deployment workflow.

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

The application requires several environment variables for integrations. These should be set in the `infra/.env` file:

```
# Telegram
CHAT_ID=your_chat_id
CHAT_TOKEN=your_bot_token
BASE_URL=https://yourdomain.com

# Tinkoff
TINKOFF_INVEST_TOKEN=your_tinkoff_token

# Finam
FINAM_API_KEY=your_finam_api_key
FINAM_ACCOUNT_KEY=your_finam_account_id

# Bybit
BYBIT_KEY=your_bybit_key
BYBIT_SECRET=your_bybit_secret

# AI Services (optional)
CLOUDRU_API_KEY=your_cloudru_key
```

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