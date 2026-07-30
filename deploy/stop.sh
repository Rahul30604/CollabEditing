#!/bin/bash
# =============================================================================
# CollabEditor - Stop Production
# =============================================================================

echo "Stopping CollabEditor..."

# Stop backend
pkill -f "collabeditor-backend" 2>/dev/null && echo "Backend stopped" || echo "Backend not running"

# Stop Docker containers
cd "$(dirname "$0")"
docker-compose down && echo "Infrastructure stopped" || echo "Docker containers not running"

echo "Done."
