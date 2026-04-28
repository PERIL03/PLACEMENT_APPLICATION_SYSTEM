#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MYSQL_HOME="${ROOT_DIR}/.local-mysql"
DATA_DIR="${MYSQL_HOME}/data"
RUN_DIR="${MYSQL_HOME}/run"
LOG_FILE="${MYSQL_HOME}/mysql.log"
SOCKET_FILE="${RUN_DIR}/mysql.sock"
PID_FILE="${RUN_DIR}/mysqld.pid"
PORT="${MYSQL_PORT:-3307}"
MYSQLD_BIN="${MYSQLD_BIN:-$(command -v mysqld || true)}"
MYSQL_BIN="${MYSQL_BIN:-$(command -v mysql || true)}"

if [[ -z "${MYSQLD_BIN}" || -z "${MYSQL_BIN}" ]]; then
  echo "Error: mysqld or mysql binary not found in PATH."
  exit 1
fi

mkdir -p "${DATA_DIR}" "${RUN_DIR}"

if [[ ! -d "${DATA_DIR}/mysql" ]]; then
  echo "Initializing local MySQL data directory at ${DATA_DIR}"
  "${MYSQLD_BIN}" --initialize-insecure --datadir="${DATA_DIR}" > /dev/null 2>&1
fi

if [[ -f "${PID_FILE}" ]] && kill -0 "$(cat "${PID_FILE}")" 2>/dev/null; then
  echo "MySQL is already running on port ${PORT}."
  exit 0
fi

echo "Starting local MySQL on port ${PORT}"
"${MYSQLD_BIN}" \
  --datadir="${DATA_DIR}" \
  --socket="${SOCKET_FILE}" \
  --pid-file="${PID_FILE}" \
  --port="${PORT}" \
  --bind-address=127.0.0.1 \
  --mysqlx=0 \
  --log-error="${LOG_FILE}" \
  --skip-networking=0 \
  --skip-log-bin \
  --daemonize

for _ in {1..40}; do
  if "${MYSQL_BIN}" -h127.0.0.1 -P"${PORT}" -uroot --protocol=tcp -e "SELECT 1" > /dev/null 2>&1; then
    break
  fi
  sleep 0.25
done

"${MYSQL_BIN}" -h127.0.0.1 -P"${PORT}" -uroot --protocol=tcp <<SQL
CREATE DATABASE IF NOT EXISTS placement_db;
CREATE USER IF NOT EXISTS 'placement_user'@'localhost' IDENTIFIED BY 'placement123';
CREATE USER IF NOT EXISTS 'placement_user'@'127.0.0.1' IDENTIFIED BY 'placement123';
GRANT ALL PRIVILEGES ON placement_db.* TO 'placement_user'@'localhost';
GRANT ALL PRIVILEGES ON placement_db.* TO 'placement_user'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL

echo "Local MySQL started."
echo "Host: 127.0.0.1"
echo "Port: ${PORT}"
echo "Database: placement_db"
echo "User: placement_user"
echo "Password: placement123"
