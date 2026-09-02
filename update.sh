#!/usr/bin/env bash
# ==============================================================================
# Attendance System - Automated Server Update & Deployment Script
# ==============================================================================
set -euo pipefail

# --- Color Definitions ---
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color
BOLD='\033[1m'

SERVICE_NAME="attendancesystem"
DEST_JAR_PATH="/opt/springapp/app.jar"
APP_PORT=8083

echo -e "${CYAN}${BOLD}======================================================${NC}"
echo -e "${CYAN}${BOLD}   🚀 Starting Attendance System Server Update       ${NC}"
echo -e "${CYAN}${BOLD}======================================================${NC}"

# 1. Ensure we are in the repository directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

echo -e "\n${YELLOW}[1/6] 📥 Pulling latest changes from Git (origin/main)...${NC}"
git pull origin main

# 2. Setup Java Environment if not set
if [ -z "${JAVA_HOME:-}" ]; then
    if [ -d "/usr/lib/jvm/java-21-openjdk" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-21-openjdk"
    elif [ -d "/usr/lib/jvm/default-runtime" ]; then
        export JAVA_HOME="/usr/lib/jvm/default-runtime"
    fi
fi

if [ -n "${JAVA_HOME:-}" ]; then
    echo -e "${CYAN}ℹ️ Using JAVA_HOME: ${JAVA_HOME}${NC}"
fi

# 3. Clean and build the application
echo -e "\n${YELLOW}[2/6] 🔨 Building Spring Boot application with Gradle...${NC}"
./gradlew clean build -x test

# 4. Locate and verify the built JAR
SOURCE_JAR="build/libs/AttendanceSystem-0.0.1-SNAPSHOT.jar"
if [ ! -f "${SOURCE_JAR}" ]; then
    # Fallback search for any non-plain executable JAR
    SOURCE_JAR=$(find build/libs -name "*.jar" ! -name "*-plain.jar" | head -n 1)
fi

if [ -z "${SOURCE_JAR}" ] || [ ! -f "${SOURCE_JAR}" ]; then
    echo -e "${RED}❌ Error: Built JAR file could not be found in build/libs/! Build aborted.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Build successful: ${SOURCE_JAR}${NC}"

# 5. Copy JAR to deployment target
echo -e "\n${YELLOW}[3/6] 📦 Deploying new JAR to ${DEST_JAR_PATH}...${NC}"
sudo mkdir -p "$(dirname "${DEST_JAR_PATH}")"
sudo cp "${SOURCE_JAR}" "${DEST_JAR_PATH}"
echo -e "${GREEN}✅ Copied JAR to ${DEST_JAR_PATH}${NC}"

# 6. Restart systemd service
echo -e "\n${YELLOW}[4/6] 🔄 Restarting systemd service: ${SERVICE_NAME}...${NC}"
sudo systemctl restart "${SERVICE_NAME}"
echo -e "${GREEN}✅ Service restart command issued.${NC}"

# 7. Wait and progress countdown for Spring Boot bootstrap
WAIT_SECONDS=6
echo -e "\n${YELLOW}[5/6] ⏳ Waiting ${WAIT_SECONDS} seconds for Spring Boot to initialize...${NC}"
for ((i=WAIT_SECONDS; i>=1; i--)); do
    echo -ne "   Starting in ${i}s... \r"
    sleep 1
done
echo -e "   Initialization complete!        "

# 8. Check service status
echo -e "\n${YELLOW}[6/6] 📊 Checking service status and health...${NC}"
if systemctl is-active --quiet "${SERVICE_NAME}"; then
    echo -e "${GREEN}${BOLD}✅ Service '${SERVICE_NAME}' is ACTIVE & RUNNING!${NC}\n"
else
    echo -e "${RED}${BOLD}⚠️ Warning: Service '${SERVICE_NAME}' is not active! Check logs below.${NC}\n"
fi

# Print latest status logs
echo -e "${CYAN}--- Recent Service Logs ---${NC}"
sudo systemctl status "${SERVICE_NAME}" --no-pager -n 15 || true

echo -e "\n${CYAN}${BOLD}======================================================${NC}"
echo -e "${GREEN}${BOLD}   🎉 Update & Deployment Process Completed!         ${NC}"
echo -e "${CYAN}${BOLD}======================================================${NC}"
