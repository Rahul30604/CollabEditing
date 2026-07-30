# CollabEditor - AWS EC2 Deployment Guide

## Architecture

```
Internet → EC2 (t3.small) → Nginx (port 80)
                               ├── / (frontend static files)
                               ├── /api/* (proxy → Spring Boot :8080)
                               └── /ws (WebSocket proxy → Spring Boot :8080)

                             Docker Compose (internal):
                               ├── MySQL :3306
                               ├── RabbitMQ :5672
                               ├── Redis :6379
                               └── ChromaDB :8000
```

## Step 1: Launch EC2 Instance

1. Go to AWS Console → EC2 → Launch Instance
2. Settings:
   - **Name:** collabeditor
   - **AMI:** Amazon Linux 2023 (or Ubuntu 22.04)
   - **Instance type:** t3.small (2 vCPU, 2GB RAM) — or t2.micro for free tier
   - **Key pair:** Create or select one (you'll need this to SSH)
   - **Storage:** 20 GB gp3
3. Security Group — allow these inbound rules:
   - SSH (port 22) — your IP only
   - HTTP (port 80) — 0.0.0.0/0
   - HTTPS (port 443) — 0.0.0.0/0 (for future SSL)
4. Launch and note the **Public IPv4 address**

## Step 2: SSH into the Instance

```bash
chmod 400 your-key.pem
ssh -i your-key.pem ec2-user@<your-ec2-ip>
```

(Use `ubuntu` instead of `ec2-user` if you chose Ubuntu AMI)

## Step 3: Run Setup Script

```bash
# Clone the repo
git clone https://github.com/Rahul30604/CollabEditing.git ~/collabeditor

# Run install script
cd ~/collabeditor/deploy
chmod +x install.sh start.sh stop.sh
./install.sh

# IMPORTANT: Log out and back in for docker group
exit
ssh -i your-key.pem ec2-user@<your-ec2-ip>
```

## Step 4: Configure Environment

```bash
cd ~/collabeditor/deploy
cp .env.example .env
nano .env
```

Fill in:
- `DOMAIN` = your EC2 public IP (e.g., 54.123.45.67)
- `OPENAI_API_KEY` = your OpenAI key
- Change all passwords to strong unique values

## Step 5: Deploy

```bash
cd ~/collabeditor/deploy
./start.sh
```

Wait 2-3 minutes for everything to build and start.

## Step 6: Access

Open in browser: `http://<your-ec2-ip>`

## Useful Commands

```bash
# View backend logs
tail -f /var/log/collabeditor.log

# Check Docker containers
docker ps

# Restart everything
cd ~/collabeditor/deploy
./stop.sh
./start.sh

# Update code and redeploy
cd ~/collabeditor
git pull
cd deploy
./stop.sh
./start.sh
```

## Troubleshooting

**Backend won't start:**
```bash
# Check if port is in use
lsof -i:8080
# Check logs
tail -50 /var/log/collabeditor.log
```

**MySQL not connecting:**
```bash
docker logs collabeditor-mysql
```

**Frontend not loading:**
```bash
sudo nginx -t
sudo systemctl status nginx
ls /var/www/collabeditor/index.html
```

**WebSocket not connecting:**
- Make sure Nginx config has the /ws proxy block
- Check browser console for WebSocket errors

## Adding SSL (HTTPS) — Optional

```bash
# Install certbot
sudo yum install -y certbot python3-certbot-nginx

# Get certificate (replace with your domain)
sudo certbot --nginx -d yourdomain.com

# Auto-renewal
sudo certbot renew --dry-run
```

## Cost

- **t2.micro (free tier):** $0/month for 12 months — tight on RAM but works for demo
- **t3.small:** ~$8/month — recommended for this stack
- **Storage (20GB):** ~$1.60/month
- **Data transfer:** first 100GB/month free

Total: **$0 (free tier)** or **~$10/month (t3.small)**
