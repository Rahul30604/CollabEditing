#!/bin/bash
# =============================================================================
# CollabEditor - EC2 Setup Script
# Run this on a fresh Amazon Linux 2023 or Ubuntu 22.04 EC2 instance
# =============================================================================

set -e

echo "========================================="
echo " CollabEditor - EC2 Setup"
echo "========================================="

# Update system
echo "[1/7] Updating system..."
sudo yum update -y 2>/dev/null || sudo apt-get update -y

# Install Docker
echo "[2/7] Installing Docker..."
if command -v yum &> /dev/null; then
    sudo yum install -y docker
    sudo systemctl start docker
    sudo systemctl enable docker
else
    sudo apt-get install -y docker.io
    sudo systemctl start docker
    sudo systemctl enable docker
fi
sudo usermod -aG docker $USER

# Install Docker Compose
echo "[3/7] Installing Docker Compose..."
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Install Java 21
echo "[4/7] Installing Java 21..."
if command -v yum &> /dev/null; then
    sudo yum install -y java-21-amazon-corretto-devel
else
    sudo apt-get install -y openjdk-21-jdk
fi

# Install Maven
echo "[5/7] Installing Maven..."
if command -v yum &> /dev/null; then
    sudo yum install -y maven
else
    sudo apt-get install -y maven
fi

# Install Nginx
echo "[6/7] Installing Nginx..."
if command -v yum &> /dev/null; then
    sudo yum install -y nginx
    sudo systemctl start nginx
    sudo systemctl enable nginx
else
    sudo apt-get install -y nginx
    sudo systemctl start nginx
    sudo systemctl enable nginx
fi

# Install Git
echo "[7/7] Installing Git..."
if command -v yum &> /dev/null; then
    sudo yum install -y git
else
    sudo apt-get install -y git
fi

echo ""
echo "========================================="
echo " Setup Complete!"
echo "========================================="
echo ""
echo "Next steps:"
echo "  1. Log out and back in (for docker group)"
echo "  2. Clone your repo: git clone <your-repo-url> ~/collabeditor"
echo "  3. cd ~/collabeditor/deploy"
echo "  4. cp .env.example .env && edit .env"
echo "  5. ./start.sh"
echo ""
