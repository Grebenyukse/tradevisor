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
   # ... export all other required variables
   ```

2. Run the secure deployment script:
   ```bash
   chmod +x deploy-secure.sh
   ./deploy-secure.sh
   ```

This approach ensures that no sensitive data is stored in files on the server filesystem, reducing the risk of accidental exposure.