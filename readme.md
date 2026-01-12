# TradeVisor Application

TradeVisor is a trading advisor application that analyzes market data and provides trading signals.

## Deployment

This application is designed to be deployed using Docker and Docker Compose on a Ubuntu 22.04 server with the following specifications:
- 4GB RAM
- 2 CPU cores
- 30GB disk space

### Prerequisites

1. Ubuntu 22.04 server
2. Docker and Docker Compose installed
3. Domain name for SSL configuration

### Deployment Steps

1. Clone the repository to your server:
   ```bash
   git clone <repository-url>
   cd tradevisor
   ```

2. Copy the environment template and configure it:
   ```bash
   cp infra/.env.template infra/.env
   # Edit infra/.env with your configuration
   ```

3. Install Docker using the provided script:
   ```bash
   chmod +x infra/scripts/install-docker.sh
   ./infra/scripts/install-docker.sh
   ```

4. Start the application:
   ```bash
   cd infra
   docker compose up -d
   ```

### Components

The deployment consists of the following components:

1. **TradeVisor Application** - The main Java application
2. **PostgreSQL** - Database for storing market data and signals
3. **Nginx** - Reverse proxy with SSL termination
4. **Prometheus** - Metrics collection
5. **Grafana** - Metrics visualization

### Resource Allocation

Containers are configured with resource limits to fit within the server's 4GB RAM and 2 CPU constraints:

- PostgreSQL: 1GB RAM, 0.5 CPU
- TradeVisor App: 2GB RAM, 1.0 CPU
- Nginx: 512MB RAM, 0.5 CPU

### SSL Configuration

To enable SSL, place your certificate and private key in `infra/nginx/certs/`:
- Certificate: `cert.pem`
- Private key: `key.pem`

Update the domain name in `infra/nginx/conf.d/tradevisor.conf` to match your domain.

### Monitoring

- Prometheus: http://your-server:9090
- Grafana: http://your-server:3000

### CI/CD

The project includes a GitHub Actions workflow for automated building and deployment. Configure the following secrets in your GitHub repository:
- `SSH_PRIVATE_KEY`: Private SSH key for server access
- `SERVER_USER`: Username for SSH access
- `SERVER_IP`: IP address of your server