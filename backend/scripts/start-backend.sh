#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_HOME="$(cd "${SCRIPT_DIR}/.." && pwd)"
APP_NAME="${APP_NAME:-bridgeability-backend}"
JAVA_BIN="${JAVA_BIN:-java}"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
APP_TIMEZONE="${APP_TIMEZONE:-Asia/Shanghai}"
APP_LOG_DIR="${APP_LOG_DIR:-${APP_HOME}/logs}"
APP_RUN_DIR="${APP_RUN_DIR:-${APP_HOME}/run}"
JVM_HEAP_DUMP_PATH="${JVM_HEAP_DUMP_PATH:-${APP_LOG_DIR}/heapdump.hprof}"
JVM_GC_LOG_FILE="${JVM_GC_LOG_FILE:-${APP_LOG_DIR}/gc.log}"

mkdir -p "${APP_LOG_DIR}" "${APP_RUN_DIR}"

if [[ -n "${APP_JAR:-}" ]]; then
  JAR_PATH="${APP_JAR}"
else
  TARGET_DIR="${APP_HOME}/target"
  if [[ -d "${TARGET_DIR}" ]]; then
    JAR_PATH="$(find "${TARGET_DIR}" -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | sort | tail -n 1)"
  else
    JAR_PATH=""
  fi
fi

if [[ -z "${JAR_PATH}" || ! -f "${JAR_PATH}" ]]; then
  echo "No runnable jar found. Build the project first with: mvn -DskipTests package" >&2
  exit 1
fi

JVM_MEMORY_OPTS="${JVM_MEMORY_OPTS:--XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=70.0}"
JVM_GC_OPTS="${JVM_GC_OPTS:--XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200}"
JVM_DIAG_OPTS="${JVM_DIAG_OPTS:--XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${JVM_HEAP_DUMP_PATH} -XX:+ExitOnOutOfMemoryError -Xlog:gc*:file=${JVM_GC_LOG_FILE}:time,uptime,level,tags:filecount=5,filesize=20M}"
JVM_SYSTEM_OPTS="${JVM_SYSTEM_OPTS:--Dfile.encoding=UTF-8 -Djava.awt.headless=true -Duser.timezone=${APP_TIMEZONE}}"

declare -a CMD
CMD=("${JAVA_BIN}")

append_words() {
  local value="${1:-}"
  if [[ -n "${value}" ]]; then
    read -r -a words <<< "${value}"
    CMD+=("${words[@]}")
  fi
}

append_words "${JVM_MEMORY_OPTS}"
append_words "${JVM_GC_OPTS}"
append_words "${JVM_DIAG_OPTS}"
append_words "${JVM_SYSTEM_OPTS}"
append_words "${JAVA_OPTS:-}"

CMD+=("-jar" "${JAR_PATH}" "--spring.profiles.active=${SPRING_PROFILES_ACTIVE}")
append_words "${SPRING_ARGS:-}"

echo "Starting ${APP_NAME}"
echo "Jar: ${JAR_PATH}"
echo "Profile: ${SPRING_PROFILES_ACTIVE}"
echo "Logs: ${APP_LOG_DIR}"

exec "${CMD[@]}"
