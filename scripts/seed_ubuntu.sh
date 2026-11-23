#!/usr/bin/env bash
# ==============================================================================
# Assignment Manager - Ubuntu Linux MySQL Auto-Setup & Seeding Script
#
# This script:
# 1. Checks if MySQL/MariaDB is installed (offers to auto-install if missing).
# 2. Ensures the MySQL service is started and active.
# 3. Creates the database `assignment_db`.
# 4. Creates a dedicated user `assignment_user` (password: assignment_pass).
# 5. Executes database/schema.sql to build tables and seed sample data.
# 6. Updates or generates `db.properties` for instant plug-and-play.
# ==============================================================================

set -e

# ANSI Color codes
BOLD='\033[1m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BOLD}${CYAN}"
echo "=========================================================="
echo "      Assignment Manager - MySQL Ubuntu Seeder"
echo "=========================================================="
echo -e "${NC}"

# Find script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
if [ ! -f "$PROJECT_ROOT/database/schema.sql" ] && [ -f "$SCRIPT_DIR/../database/schema.sql" ]; then
    PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
fi

SCHEMA_FILE="$PROJECT_ROOT/database/schema.sql"
CONFIG_FILE="$PROJECT_ROOT/db.properties"

if [ ! -f "$SCHEMA_FILE" ]; then
    echo -e "${RED}[ERROR] schema.sql not found at: $SCHEMA_FILE${NC}"
    exit 1
fi

echo -e "${CYAN}[INFO] Project Root: $PROJECT_ROOT${NC}"

# 1. Check if MySQL / MariaDB is installed
if ! command -v mysql &> /dev/null; then
    echo -e "${YELLOW}[!] MySQL is not currently installed on this system.${NC}"
    read -p "Would you like to install mysql-server now via apt? [Y/n]: " INSTALL_CHOICE
    INSTALL_CHOICE=${INSTALL_CHOICE:-Y}
    if [[ "$INSTALL_CHOICE" =~ ^[Yy]$ ]]; then
        echo -e "${CYAN}[INFO] Updating package lists and installing mysql-server...${NC}"
        sudo apt-get update
        sudo apt-get install -y mysql-server
    else
        echo -e "${RED}[ABORT] MySQL is required to seed the database. Exiting.${NC}"
        exit 1
    fi
fi

# 2. Ensure MySQL service is running
echo -e "${CYAN}[INFO] Checking MySQL service status...${NC}"
if ! systemctl is-active --quiet mysql; then
    echo -e "${YELLOW}[INFO] Starting MySQL service...${NC}"
    sudo systemctl start mysql
fi

echo -e "${GREEN}[✔] MySQL service is active and running.${NC}"

# 3. Setup Database, Tables, Seed Data and Dedicated User
DB_NAME="assignment_db"
DB_USER="assignment_user"
DB_PASS="assignment_pass"

echo -e "${CYAN}[INFO] Seeding database schema and initial records...${NC}"

# Use sudo mysql which leverages auth_socket on Ubuntu without prompting for root password
sudo mysql <<EOF
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';
ALTER USER '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;
EOF

# Execute the schema.sql file
sudo mysql "${DB_NAME}" < "$SCHEMA_FILE"

echo -e "${GREEN}[✔] Database and tables successfully created and seeded!${NC}"

# 4. Verify seeded records
RECORD_COUNT=$(sudo mysql -N -e "SELECT COUNT(*) FROM assignments;" "${DB_NAME}" 2>/dev/null || echo "0")
echo -e "${GREEN}[✔] Verified: ${RECORD_COUNT} assignment records are ready in '${DB_NAME}'.${NC}"

# 5. Generate / Update db.properties for the Java application
echo -e "${CYAN}[INFO] Updating db.properties with local connection credentials...${NC}"
cat <<EOF > "$CONFIG_FILE"
# Database Configuration for Assignment Manager
db.host=localhost
db.port=3306
db.name=${DB_NAME}
db.username=${DB_USER}
db.password=${DB_PASS}
EOF

echo -e "${GREEN}[✔] Saved credentials to $CONFIG_FILE${NC}"

# 6. Completion Summary
echo ""
echo -e "${BOLD}${GREEN}==========================================================${NC}"
echo -e "${BOLD}${GREEN}   Database Setup & Seeding Completed Successfully!       ${NC}"
echo -e "${BOLD}${GREEN}==========================================================${NC}"
echo -e "${CYAN}Connection Details:${NC}"
echo -e "  Host:     ${BOLD}localhost${NC}"
echo -e "  Port:     ${BOLD}3306${NC}"
echo -e "  Database: ${BOLD}${DB_NAME}${NC}"
echo -e "  Username: ${BOLD}${DB_USER}${NC}"
echo -e "  Password: ${BOLD}${DB_PASS}${NC}"
echo ""
echo -e "${YELLOW}To launch the Assignment Manager on Ubuntu, simply run:${NC}"
echo -e "  ${BOLD}./run.sh${NC}"
echo ""
