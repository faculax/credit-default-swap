#!/bin/bash
#
# SonarQube Setup Script for CDS Platform
# Sets up SonarQube Community Edition with PostgreSQL
#

set -e

# Colors
BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                                                            ║${NC}"
echo -e "${BLUE}║          SonarQube Setup for CDS Platform                 ║${NC}"
echo -e "${BLUE}║                                                            ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running. Please start Docker and try again.${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Docker is running${NC}"
echo ""

# Check system requirements
echo -e "${BLUE}📋 Checking system requirements...${NC}"
echo ""

# Check available memory
available_memory=$(free -m | awk '/^Mem:/{print $7}')
if [ "$available_memory" -lt 2048 ]; then
    echo -e "${YELLOW}⚠️  Warning: Less than 2GB memory available. SonarQube may run slowly.${NC}"
else
    echo -e "${GREEN}✓ Memory: ${available_memory}MB available${NC}"
fi

# Check vm.max_map_count for Elasticsearch
if [ -f /proc/sys/vm/max_map_count ]; then
    current_max_map_count=$(cat /proc/sys/vm/max_map_count)
    if [ "$current_max_map_count" -lt 262144 ]; then
        echo -e "${YELLOW}⚠️  Setting vm.max_map_count for Elasticsearch...${NC}"
        sudo sysctl -w vm.max_map_count=262144
        echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf > /dev/null
        echo -e "${GREEN}✓ vm.max_map_count set to 262144${NC}"
    else
        echo -e "${GREEN}✓ vm.max_map_count: $current_max_map_count (sufficient)${NC}"
    fi
fi

echo ""

# Start SonarQube
echo -e "${BLUE}🚀 Starting SonarQube...${NC}"
echo -e "${YELLOW}This may take 2-3 minutes on first run...${NC}"
echo ""

docker-compose -f docker-compose.sonarqube.yml up -d

echo ""
echo -e "${YELLOW}⏳ Waiting for SonarQube to initialize...${NC}"
echo ""

# Wait for SonarQube to be ready
retry_count=0
max_retries=60

while [ $retry_count -lt $max_retries ]; do
    if curl -s http://localhost:9000/api/system/status | grep -q '"status":"UP"'; then
        break
    fi
    echo -n "."
    sleep 5
    ((retry_count++))
done

echo ""
echo ""

if [ $retry_count -ge $max_retries ]; then
    echo -e "${RED}❌ SonarQube failed to start within expected time${NC}"
    echo -e "${YELLOW}Check logs with: docker-compose -f docker-compose.sonarqube.yml logs${NC}"
    exit 1
fi

echo -e "${GREEN}✅ SonarQube is ready!${NC}"
echo ""

# Display access information
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}📍 Access Information${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "   ${BLUE}🌐 URL:      ${NC}http://localhost:9000"
echo -e "   ${BLUE}👤 Username: ${NC}admin"
echo -e "   ${BLUE}🔑 Password: ${NC}admin"
echo ""
echo -e "   ${YELLOW}⚠️  IMPORTANT: Change the default password immediately!${NC}"
echo ""

# Prompt to open browser
read -p "Would you like to open SonarQube in your browser? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if command -v xdg-open > /dev/null; then
        xdg-open http://localhost:9000
    elif command -v open > /dev/null; then
        open http://localhost:9000
    else
        echo -e "${YELLOW}Please open http://localhost:9000 in your browser${NC}"
    fi
fi

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}📚 Next Steps${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "   1. Login to SonarQube at http://localhost:9000"
echo "   2. Change admin password:"
echo "      → User Menu (top right) → My Account → Security → Change Password"
echo ""
echo "   3. Generate an API token:"
echo "      → User Menu → My Account → Security → Generate Tokens"
echo "      → Name: 'GitHub Actions'"
echo "      → Type: 'Global Analysis Token' or 'User Token'"
echo "      → Click 'Generate' → Copy the token"
echo ""
echo "   4. Add GitHub Secrets:"
echo "      → Repository Settings → Secrets and variables → Actions"
echo "      → Add: SONAR_HOST_URL = http://localhost:9000"
echo "      → Add: SONAR_TOKEN = <your-token-from-step-3>"
echo ""
echo "   5. (Optional) Add Snyk token for dependency scanning:"
echo "      → Sign up at https://snyk.io (free tier available)"
echo "      → Get API token from Account Settings"
echo "      → Add: SNYK_TOKEN = <your-snyk-token>"
echo ""
echo "   6. Run your first analysis:"
echo "      → Push code to trigger GitHub Actions workflow"
echo "      → OR run locally:"
echo "        cd backend"
echo "        ./mvnw clean verify sonar:sonar \\"
echo "          -Dsonar.host.url=http://localhost:9000 \\"
echo "          -Dsonar.token=<your-token>"
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}🛠️  Useful Commands${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "   ${YELLOW}View logs:${NC}"
echo "   docker-compose -f docker-compose.sonarqube.yml logs -f"
echo ""
echo -e "   ${YELLOW}Stop SonarQube:${NC}"
echo "   docker-compose -f docker-compose.sonarqube.yml down"
echo ""
echo -e "   ${YELLOW}Restart SonarQube:${NC}"
echo "   docker-compose -f docker-compose.sonarqube.yml restart"
echo ""
echo -e "   ${YELLOW}Remove all data (reset):${NC}"
echo "   docker-compose -f docker-compose.sonarqube.yml down -v"
echo ""
echo -e "   ${YELLOW}Check SonarQube status:${NC}"
echo "   curl http://localhost:9000/api/system/status"
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${GREEN}✅ Setup complete! SonarQube is ready for code analysis.${NC}"
echo ""
