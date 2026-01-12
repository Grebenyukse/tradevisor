#!/bin/bash

# Script to install Docker on Ubuntu 22.04
# This script will install Docker Engine, Docker Compose, and perform basic configuration

set -e  # Exit immediately if a command exits with a non-zero status

echo "Starting Docker installation on Ubuntu 22.04..."

# Update package index
echo "Updating package index..."
sudo apt-get update

# Install prerequisites
echo "Installing prerequisites..."
sudo apt-get install -y \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

# Add Docker's official GPG key
echo "Adding Docker's official GPG key..."
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Set up the repository
echo "Setting up Docker repository..."
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Update package index again
echo "Updating package index with Docker repository..."
sudo apt-get update

# Install Docker Engine
echo "Installing Docker Engine..."
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Start and enable Docker service
echo "Starting and enabling Docker service..."
sudo systemctl start docker
sudo systemctl enable docker

# Add current user to docker group
echo "Adding current user to docker group..."
sudo usermod -aG docker $USER

# Install Docker Compose (legacy version for compatibility)
echo "Installing Docker Compose..."
sudo apt-get install -y docker-compose

# Verify installation
echo "Verifying Docker installation..."
docker --version
docker compose version

echo "Docker installation completed successfully!"
echo "Please log out and log back in for the group changes to take effect."
echo "After logging back in, you can verify the installation by running:"
echo "  docker run hello-world"