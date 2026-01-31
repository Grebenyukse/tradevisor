#!/bin/bash

set -e

CERT_PATH="/etc/letsencrypt/live/grebenyukse.ru"
TEMPLATE_FILE="/etc/nginx/conf.d/tradevisor-https.conf.template"
HTTPS_CONFIG="/etc/nginx/conf.d/tradevisor-https.conf"
WEBROOT_PATH="/var/www/certbot"

echo "🚀 Starting deployment process..."

# Ensure required directories exist
mkdir -p "$WEBROOT_PATH"
mkdir -p "$(dirname "$CERT_PATH")"

# Step 1: Remove any existing HTTPS config to avoid conflicts
echo "🧹 Cleaning up previous HTTPS config..."
rm -f "$HTTPS_CONFIG"

# Step 2: Check if certificate already exists
if [ -f "$CERT_PATH/fullchain.pem" ]; then
    echo "✅ Certificate found. Enabling HTTPS immediately."

    # Generate HTTPS config from template
    echo "📄 Generating HTTPS config from template..."
    cp "$TEMPLATE_FILE" "$HTTPS_CONFIG"

    # Start Nginx if not running
    if ! pgrep nginx > /dev/null; then
        echo "🟢 Starting Nginx..."
        nginx
    else
        echo "🔁 Reloading Nginx with HTTPS support..."
        nginx -s reload
    fi

else
    echo "⚠️  No certificate found. Obtaining one now..."

    # Remove HTTPS config temporarily
    rm -f "$HTTPS_CONFIG"

    # Start Nginx with HTTP-only config
    echo "🟢 Starting Nginx with HTTP only..."
    nginx || true

    # Give Nginx time to start
    sleep 3

    # Run Certbot to get the certificate
    echo "🔐 Running Certbot to obtain certificate..."
    certbot certonly \
      --webroot \
      --webroot-path="$WEBROOT_PATH" \
      --email bugor-p@yandex.ru \
      --agree-tos \
      --no-eff-email \
      -d grebenyukse.ru \
      -d www.grebenyukse.ru \
      --non-interactive

    # Wait until certificate is available
    echo "⏳ Waiting for certificate to appear..."
    while [ ! -f "$CERT_PATH/fullchain.pem" ]; do
        echo "⏳ Still waiting..."
        sleep 5
    done

    # Now generate HTTPS config and enable it
    echo "📄 Generating HTTPS config from template..."
    cp "$TEMPLATE_FILE" "$HTTPS_CONFIG"

    # Reload Nginx with HTTPS support
    echo "🔁 Reloading Nginx with HTTPS support..."
    nginx -s reload
fi

echo "✅ Deployment completed successfully!"

# Keep container alive
tail -f /dev/null
