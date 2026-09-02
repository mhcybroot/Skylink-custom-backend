#!/usr/bin/env bash
# ==============================================================================
# Attendance System - Service Health & Log Diagnostic Reporter
# ==============================================================================
set -u

# --- Color Definitions ---
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
GRAY='\033[0;90m'
NC='\033[0m' # No Color
BOLD='\033[1m'

SERVICE_NAME="attendancesystem"
APP_PORT=8083
APP_URL="http://localhost:${APP_PORT}/login"
DB_NAME="skylink_database"
DB_PORT=5432

SHOW_ERRORS_ONLY=false
SHOW_WARNINGS_ONLY=false
SHOW_LOGS_COUNT=0
EXPORT_REPORT=false
REPORT_FILE=""

# Parse command line flags
while [[ $# -gt 0 ]]; do
    case "$1" in
        -e|--errors)
            SHOW_ERRORS_ONLY=true
            shift
            ;;
        -w|--warnings)
            SHOW_WARNINGS_ONLY=true
            shift
            ;;
        -l|--logs)
            SHOW_LOGS_COUNT="${2:-50}"
            shift 2 || shift
            ;;
        --export)
            EXPORT_REPORT=true
            REPORT_FILE="health_report_$(date +%Y%m%d_%H%M%S).txt"
            shift
            ;;
        -h|--help)
            echo -e "${BOLD}Attendance System Health Diagnostic Tool${NC}"
            echo -e "Usage: ./healthcheck.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -e, --errors     Show detailed error stack traces and critical failures"
            echo "  -w, --warnings   Show database warnings, collation notices, and deprecations"
            echo "  -l, --logs [N]   Display the last N service logs (default: 50)"
            echo "  --export         Save health report output to a text file"
            echo "  -h, --help       Show this help dialog"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}. Use --help for usage."
            exit 1
            ;;
    esac
done

if [ "$EXPORT_REPORT" = true ]; then
    exec > >(tee -a "${REPORT_FILE}") 2>&1
    echo -e "${GRAY}📁 Output will also be saved to ${REPORT_FILE}${NC}\n"
fi

echo -e "${CYAN}${BOLD}╔══════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}${BOLD}║   🩺 Attendance System Service Health & Log Diagnostic Dashboard     ║${NC}"
echo -e "${CYAN}${BOLD}╚══════════════════════════════════════════════════════════════════════╝${NC}"
echo -e "${GRAY}Report Generated: $(date '+%Y-%m-%d %H:%M:%S %Z') | Host: $(hostname)${NC}\n"

# ==============================================================================
# 1. Systemd Service Status & Process Check
# ==============================================================================
echo -e "${BOLD}1. ⚙️  Systemd Service Status${NC}"
echo -e "${GRAY}----------------------------------------------------------------------${NC}"

SERVICE_ACTIVE=false
if systemctl is-active --quiet "${SERVICE_NAME}" 2>/dev/null; then
    SERVICE_ACTIVE=true
    echo -e "  • Service Status : ${GREEN}${BOLD}● ACTIVE (RUNNING)${NC}"
else
    echo -e "  • Service Status : ${RED}${BOLD}○ INACTIVE / FAILED${NC}"
fi

MAIN_PID=$(systemctl show "${SERVICE_NAME}" -p MainPID --value 2>/dev/null || echo "0")
if [ "${MAIN_PID}" != "0" ] && [ -n "${MAIN_PID}" ]; then
    UPTIME=$(ps -p "${MAIN_PID}" -o etime= 2>/dev/null | xargs || echo "Unknown")
    MEM_USAGE=$(ps -p "${MAIN_PID}" -o rss= 2>/dev/null | awk '{printf "%.1f MB", $1/1024}' || echo "Unknown")
    CPU_USAGE=$(ps -p "${MAIN_PID}" -o %cpu= 2>/dev/null | xargs || echo "0.0")
    echo -e "  • Main Process   : PID ${BOLD}${MAIN_PID}${NC} (Uptime: ${UPTIME})"
    echo -e "  • Memory (RAM)   : ${BOLD}${MEM_USAGE}${NC}"
    echo -e "  • CPU Usage      : ${BOLD}${CPU_USAGE}%${NC}"
else
    echo -e "  • Main Process   : ${RED}No active process found for ${SERVICE_NAME}${NC}"
fi

# ==============================================================================
# 2. Network & Port HTTP Endpoint Health
# ==============================================================================
echo -e "\n${BOLD}2. 🌐 Network & HTTP Endpoint Health${NC}"
echo -e "${GRAY}----------------------------------------------------------------------${NC}"

PORT_OPEN=false
if command -v ss >/dev/null 2>&1; then
    if ss -tulpn 2>/dev/null | grep -q ":${APP_PORT}"; then
        PORT_OPEN=true
    fi
elif command -v netstat >/dev/null 2>&1; then
    if netstat -tuln 2>/dev/null | grep -q ":${APP_PORT}"; then
        PORT_OPEN=true
    fi
elif command -v lsof >/dev/null 2>&1; then
    if lsof -i ":${APP_PORT}" >/dev/null 2>&1; then
        PORT_OPEN=true
    fi
fi

if [ "$PORT_OPEN" = true ] || [ "$SERVICE_ACTIVE" = true ]; then
    echo -e "  • Port ${APP_PORT} Bind   : ${GREEN}${BOLD}✓ LISTENING${NC}"
else
    echo -e "  • Port ${APP_PORT} Bind   : ${RED}${BOLD}✗ NOT BOUND${NC}"
fi

# Probe HTTP endpoint
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 "${APP_URL}" 2>/dev/null || echo "000")
if [ "$HTTP_STATUS" = "200" ] || [ "$HTTP_STATUS" = "302" ]; then
    echo -e "  • Web Response   : ${GREEN}${BOLD}✓ HTTP ${HTTP_STATUS} (Application Ready)${NC}"
elif [ "$HTTP_STATUS" = "000" ]; then
    echo -e "  • Web Response   : ${RED}${BOLD}✗ Connection Refused / Timeout${NC}"
else
    echo -e "  • Web Response   : ${YELLOW}${BOLD}! HTTP ${HTTP_STATUS}${NC}"
fi

# ==============================================================================
# 3. Database Connectivity
# ==============================================================================
echo -e "\n${BOLD}3. 🐘 PostgreSQL Database Health${NC}"
echo -e "${GRAY}----------------------------------------------------------------------${NC}"

DB_OK=false
if command -v pg_isready >/dev/null 2>&1; then
    if pg_isready -h localhost -p "${DB_PORT}" -d "${DB_NAME}" -q 2>/dev/null; then
        DB_OK=true
    fi
fi

if [ "$DB_OK" = true ]; then
    echo -e "  • Database Link  : ${GREEN}${BOLD}✓ CONNECTED (PostgreSQL on port ${DB_PORT})${NC}"
    echo -e "  • Database Name  : ${BOLD}${DB_NAME}${NC}"
else
    # Fallback port probe
    if nc -z localhost "${DB_PORT}" 2>/dev/null || (echo >/dev/tcp/localhost/${DB_PORT}) 2>/dev/null; then
        echo -e "  • Database Port  : ${GREEN}${BOLD}✓ Port ${DB_PORT} OPEN${NC}"
    else
        echo -e "  • Database Port  : ${RED}${BOLD}✗ Port ${DB_PORT} UNREACHABLE${NC}"
    fi
fi

# ==============================================================================
# 4. System Resources (Disk & System Load)
# ==============================================================================
echo -e "\n${BOLD}4. 💾 System Host Resources${NC}"
echo -e "${GRAY}----------------------------------------------------------------------${NC}"
DISK_INFO=$(df -h / 2>/dev/null | awk 'NR==2 {print $3 " used / " $2 " total (" $5 " used)"}')
LOAD_AVG=$(uptime 2>/dev/null | awk -F'load average:' '{print $2}' | xargs || echo "Unknown")
echo -e "  • Root Disk Space: ${BOLD}${DISK_INFO}${NC}"
echo -e "  • Load Average   : ${BOLD}${LOAD_AVG}${NC}"

# ==============================================================================
# 5. Smart Log Analyzer & Diagnostics (Journalctl / Logs)
# ==============================================================================
echo -e "\n${BOLD}5. 🧠 Smart Log Analyzer & Diagnostics${NC}"
echo -e "${GRAY}----------------------------------------------------------------------${NC}"

# Gather logs from journalctl or fallback log files
LOG_DATA=""
if command -v journalctl >/dev/null 2>&1; then
    LOG_DATA=$(journalctl -u "${SERVICE_NAME}" -n 500 --no-pager 2>/dev/null || true)
fi

# If journalctl is empty, check for local spring logs or task logs
if [ -z "${LOG_DATA}" ]; then
    for candidate in "/opt/springapp/app.log" "logs/app.log" "app.log"; do
        if [ -f "${candidate}" ]; then
            LOG_DATA=$(tail -n 500 "${candidate}")
            break
        fi
    done
fi

if [ -z "${LOG_DATA}" ]; then
    echo -e "  ${YELLOW}ℹ️  No journalctl or local log file found for ${SERVICE_NAME}.${NC}"
    echo -e "     (If running in development, logs are displayed on active terminal/bootRun)${NC}"
else
    # Count occurrences
    ERROR_COUNT=$(echo "${LOG_DATA}" | grep -E -i "ERROR|Exception|FATAL|SEVERE" | wc -l)
    WARN_COUNT=$(echo "${LOG_DATA}" | grep -E -i "WARN|Warning|Notice|mismatch" | grep -v -E "INFO|DEBUG" | wc -l)
    INFO_BOOT=$(echo "${LOG_DATA}" | grep -i "Started AttendanceSystemApplication" | tail -n 1 || echo "")

    echo -e "  • Error/Exception Count : $([ "${ERROR_COUNT}" -gt 0 ] && echo -e "${RED}${BOLD}${ERROR_COUNT} Issues Detected${NC}" || echo -e "${GREEN}0 (Clean)${NC}")"
    echo -e "  • Warnings/Notices Count: $([ "${WARN_COUNT}" -gt 0 ] && echo -e "${YELLOW}${BOLD}${WARN_COUNT} Notices/Warnings${NC}" || echo -e "${GREEN}0 (Clean)${NC}")"
    
    if [ -n "${INFO_BOOT}" ]; then
        echo -e "  • Last Boot Milestone   : ${GREEN}${INFO_BOOT}${NC}"
    fi

    # Display Top Errors if any
    if [ "${ERROR_COUNT}" -gt 0 ] || [ "$SHOW_ERRORS_ONLY" = true ]; then
        echo -e "\n  ${RED}${BOLD}🔴 Recent Error / Exception Snippets:${NC}"
        echo "${LOG_DATA}" | grep -E -i "ERROR|Exception|FATAL|SEVERE" -B 1 -A 2 | tail -n 15 | while IFS= read -r line; do
            echo -e "    ${RED}›${NC} ${line}"
        done
    fi

    # Display Top Warnings / Notices if any
    if [ "${WARN_COUNT}" -gt 0 ] || [ "$SHOW_WARNINGS_ONLY" = true ]; then
        echo -e "\n  ${YELLOW}${BOLD}🟡 Recent Warnings & Notices:${NC}"
        echo "${LOG_DATA}" | grep -E -i "WARN|Warning|Notice|collation" | tail -n 10 | while IFS= read -r line; do
            echo -e "    ${YELLOW}›${NC} ${line}"
        done
    fi
fi

# Tail requested logs if specified
if [ "${SHOW_LOGS_COUNT}" -gt 0 ] && [ -n "${LOG_DATA}" ]; then
    echo -e "\n${BOLD}6. 📜 Last ${SHOW_LOGS_COUNT} Log Lines${NC}"
    echo -e "${GRAY}----------------------------------------------------------------------${NC}"
    echo "${LOG_DATA}" | tail -n "${SHOW_LOGS_COUNT}"
fi

# ==============================================================================
# Summary Verdict
# ==============================================================================
echo -e "\n${CYAN}${BOLD}======================================================================${NC}"
if [ "$SERVICE_ACTIVE" = true ] && [ "$HTTP_STATUS" = "200" -o "$HTTP_STATUS" = "302" ]; then
    echo -e "${GREEN}${BOLD}   🎉 OVERALL HEALTH: HEALTHY & OPERATIONAL (100% READY)              ${NC}"
elif [ "$SERVICE_ACTIVE" = true ]; then
    echo -e "${YELLOW}${BOLD}   ⚠️ OVERALL HEALTH: SERVICE ACTIVE BUT HTTP ENDPOINT INITIALIZING    ${NC}"
else
    echo -e "${RED}${BOLD}   ❌ OVERALL HEALTH: SERVICE IS DOWN OR REQUIRES RESTART              ${NC}"
fi
echo -e "${CYAN}${BOLD}======================================================================${NC}"
