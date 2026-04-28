#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="${ROOT_DIR}/.local-mysql/run/mysqld.pid"

if [[ ! -f "${PID_FILE}" ]]; then
  echo "No local MySQL PID file found."
  exit 0
fi

PID="$(cat "${PID_FILE}")"
if kill -0 "${PID}" 2>/dev/null; then
  echo "Stopping local MySQL PID ${PID}"
  kill "${PID}"
  echo "Stopped."
else
  echo "Process not running; cleaning stale PID file."
fi

rm -f "${PID_FILE}"
