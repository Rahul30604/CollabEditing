#!/bin/bash
# =============================================================================
# CollabEditor - Start Production
# Run from the deploy/ directory
# =============================================================================

set -e

# Load environment variables
if [ ! -f .env ]; then
    echo "ERROR: .env file not found. Copy .env.example to .env and fill in values."
    exit 1
fi
source .env

echo "========================================="
echo " Starting CollabEditor Production"
echo "========================================="

# 1. Start infrastructure
echo "[1/5] Starting Docker containers..."
docker-compose up -d
echo "Waiting for services to be healthy..."
sleep 20

# 2. Build frontend
echo "[2/5] Building frontend..."
cd ../frontend
npm install
REACT_APP_API_URL="" npm run build
sudo rm -rf /var/www/collabeditor
sudo mkdir -p /var/www/collabeditor
sudo cp -r build/* /var/www/collabeditor/
cd ../deploy

# 3. Build backend
echo "[3/5] Building backend..."
cd ../backend
mvn package -DskipTests -B
cd ../deploy

# 4. Setup Nginx
echo "[4/5] Configuring Nginx..."
sudo cp nginx/collabeditor.conf /etc/nginx/conf.d/collabeditor.conf
sudo rm -f /etc/nginx/conf.d/default.conf 2>/dev/null
sudo nginx -t && sudo systemctl reload nginx

# 5. Start backend
echo "[5/5] Starting backend..."
# Stop existing backend if running
pkill -f "collabeditor-backend" 2>/dev/null || true
sleep 2

# Start backend with production config
nohup java -jar ../backend/target/collabeditor-backend-0.0.1-SNAPSHOT.jar \
    --spring.profiles.active=prod \
    --spring.datasource.url="jdbc:mysql://127.0.0.1:3306/collabeditor?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
    --spring.datasource.username="${MYSQL_USER}" \
    --spring.datasource.password="${MYSQL_PASSWORD}" \
    --spring.rabbitmq.host=127.0.0.1 \
    --spring.rabbitmq.username="${RABBITMQ_USER}" \
    --spring.rabbitmq.password="${RABBITMQ_PASS}" \
    --spring.data.redis.host=127.0.0.1 \
    --spring.data.redis.password="${REDIS_PASSWORD}" \
    --app.ai.openai.api-key="${OPENAI_API_KEY}" \
    --app.ai.chromadb.base-url="http://127.0.0.1:8000" \
    --app.jwt.secret="${JWT_SECRET}" \
    > /var/log/collabeditor.log 2>&1 &

echo ""
echo "========================================="
echo " CollabEditor is running!"
echo "========================================="
echo ""
echo " App:         http://${DOMAIN}"
echo " API:         http://${DOMAIN}/api"
echo " RabbitMQ:    http://${DOMAIN}:15672 (internal only)"
echo ""
echo " Logs: tail -f /var/log/collabeditor.log"
echo ""
