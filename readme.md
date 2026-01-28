## сервер
ssh tradevisor@45.155.205.225

## запуск telegram bot локально:
1. cd C:\Users\grebe\Downloads\clo-2.4.5-stable-windows-x86_64
2.  .\clo.exe publish http 8080


## A входные данные:
- Ai скоринг
- экономический календарь
- календарь дивидентов
- календарь экспирации фьючерсов и опционов
- календарь экономической отчетности
- календарь выплаты налогов (регуляторный)
- high season
- цены спот, фьючерс, опцион
- индикатор фибо
- волатильность

## B вычисление сигнала:
### event:
1. есть важное событие в экономическом календаре на этой неделе
2. выплаты дивидендов через месяц
3. экспирация фьючерсов через 2 недели
4. выход экономической отчетности через 2 недели
5. выплата налогов через 2 недели
6. high season

### C strategy:
1. покупка безрискового актива на свободную маржу
2. покупка stradle опциона
3. арбитраж спот/фьюч
4. one way martin gale
4. fibo:

fibo:
1. есть одно касание fibo на m5 или h1 или d1
2. расстояние актуальной цены от точки касания > 2 стандартных отклонения
3. расстояние актуальной цены от точки касания < 1 стандартного отклонения

## D trade:
1. оценка риска, вычисление размера позиции
2. тправка торгового сигнала по крипте
3. отправка торгового сигнала по акциям/фьючам/валюте
4. управление stop loss take profit.

## D notify:
1. отправка скрина с торговой позицией в telegram
2. отправка скрина с закрытой позицией в telegram
3. статистика по счету за месяц



запуск telegram bot локально:

1. cd C:\Users\grebe\Downloads\clo-2.4.5-stable-windows-x86_64
2.  .\clo.exe publish http 8080


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

2. Set environment variables directly (recommended for security):
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
   export EVENTS_THREAD_ID=events_thread_id
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