#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="${SCRIPT_DIR}/exemplul 2"

DATAFLOW_URI="${DATAFLOW_URI:-http://localhost:9393}"
STREAM_NAME="${STREAM_NAME:-lab9demo}"

DATAFLOW_HOME="${DATAFLOW_HOME:-${SCRIPT_DIR}/.dataflow}"
RUN_DIR="${DATAFLOW_HOME}/run"
LOG_DIR="${DATAFLOW_HOME}/logs"

SKIPPER_VERSION="2.4.3.RELEASE"
DATAFLOW_VERSION="2.5.4.RELEASE"

SKIPPER_JAR="${DATAFLOW_HOME}/spring-cloud-skipper-server-${SKIPPER_VERSION}.jar"
DATAFLOW_SERVER_JAR="${DATAFLOW_HOME}/spring-cloud-dataflow-server-${DATAFLOW_VERSION}.jar"

SKIPPER_PORT=7577
DATAFLOW_PORT=9393

STREAM_DEFINITION="client | comanda | depozit | facturare | livrare"
DEPLOYMENT_PROPERTIES_JSON='{
  "deployer.client.local.server.port": "9001",
  "deployer.comanda.local.server.port": "9002",
  "deployer.depozit.local.server.port": "9003",
  "deployer.facturare.local.server.port": "9004",
  "deployer.livrare.local.server.port": "9005"
}'

MODULES=("Client" "Comanda" "Depozit" "Facturare" "Livrare")

log() {
  printf '[%s] %s\n' "$(date '+%H:%M:%S')" "$*"
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Lipseste comanda necesara: $1" >&2
    exit 1
  fi
}

is_port_open() {
  local port="$1"
  lsof -nP -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1
}

wait_for_port() {
  local port="$1"
  local label="$2"
  local retries="${3:-90}"
  local i

  for ((i=1; i<=retries; i++)); do
    if is_port_open "$port"; then
      return 0
    fi
    sleep 1
  done

  echo "Timeout la pornirea ${label} pe portul ${port}." >&2
  return 1
}

download_if_missing() {
  local url="$1"
  local out="$2"
  if [[ -f "$out" ]]; then
    return 0
  fi
  log "Descarc $(basename "$out") ..."
  curl -fL "$url" -o "$out"
}

ensure_rabbitmq() {
  if is_port_open 5672; then
    log "RabbitMQ este deja pornit (port 5672 activ)."
    return 0
  fi

  if rabbitmqctl status >/dev/null 2>&1; then
    log "RabbitMQ este deja pornit."
    return 0
  fi

  if command -v brew >/dev/null 2>&1; then
    log "Pornesc RabbitMQ din brew services..."
    brew services start rabbitmq >/dev/null
    sleep 2
    if is_port_open 5672; then
      log "RabbitMQ este pornit."
      return 0
    fi

    rabbitmqctl status >/dev/null 2>&1 || {
      echo "RabbitMQ nu a pornit corect." >&2
      exit 1
    }
    log "RabbitMQ este pornit."
    return 0
  fi

  echo "RabbitMQ nu este pornit si nu exista 'brew' pentru restart automat." >&2
  exit 1
}

ensure_dataflow_jars() {
  mkdir -p "$DATAFLOW_HOME"

  download_if_missing \
    "https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-skipper-server/${SKIPPER_VERSION}/spring-cloud-skipper-server-${SKIPPER_VERSION}.jar" \
    "$SKIPPER_JAR"

  download_if_missing \
    "https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-dataflow-server/${DATAFLOW_VERSION}/spring-cloud-dataflow-server-${DATAFLOW_VERSION}.jar" \
    "$DATAFLOW_SERVER_JAR"
}

start_component_if_needed() {
  local name="$1"
  local jar="$2"
  local port="$3"
  local pid_file="${RUN_DIR}/${name}.pid"
  local log_file="${LOG_DIR}/${name}.log"

  if is_port_open "$port"; then
    log "${name} este deja pornit pe portul ${port}."
    return 0
  fi

  log "Pornesc ${name}..."
  nohup java -jar "$jar" >"$log_file" 2>&1 &
  echo "$!" >"$pid_file"

  if ! wait_for_port "$port" "$name"; then
    echo "--- Ultimele linii din ${log_file} ---" >&2
    tail -n 40 "$log_file" >&2 || true
    exit 1
  fi

  log "${name} a pornit."
}

wait_for_dataflow_api() {
  local retries=90
  local i
  for ((i=1; i<=retries; i++)); do
    if curl -fsS "${DATAFLOW_URI}/about" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done

  echo "Data Flow API nu raspunde la ${DATAFLOW_URI}/about." >&2
  return 1
}

build_apps() {
  local module
  for module in "${MODULES[@]}"; do
    log "Build ${module}..."
    (cd "${LAB2_DIR}/${module}" && mvn -q package)
  done
}

jar_uri() {
  local jar_path="$1"
  local encoded_path=""
  local i ch hex

  for ((i=0; i<${#jar_path}; i++)); do
    ch="${jar_path:i:1}"
    case "$ch" in
      [a-zA-Z0-9.~_/-])
        encoded_path+="$ch"
        ;;
      *)
        printf -v hex '%02X' "'$ch"
        encoded_path+="%${hex}"
        ;;
    esac
  done

  printf 'file://%s' "$encoded_path"
}

register_app() {
  local type="$1"
  local name="$2"
  local jar_path="$3"

  if [[ ! -f "$jar_path" ]]; then
    echo "Lipseste jar-ul: $jar_path" >&2
    exit 1
  fi

  log "Register ${type}:${name}"
  curl -fsS -X POST "${DATAFLOW_URI}/apps/${type}/${name}" \
    --data-urlencode "uri=$(jar_uri "$jar_path")" \
    --data-urlencode "force=true" \
    >/dev/null
}

prepare_stream() {
  log "Curat stream vechi (daca exista): ${STREAM_NAME}"
  curl -fsS -X DELETE "${DATAFLOW_URI}/streams/deployments/${STREAM_NAME}" >/dev/null 2>&1 || true
  curl -fsS -X DELETE "${DATAFLOW_URI}/streams/definitions/${STREAM_NAME}" >/dev/null 2>&1 || true

  log "Creez stream: ${STREAM_NAME}"
  curl -fsS -X POST "${DATAFLOW_URI}/streams/definitions" \
    --data-urlencode "name=${STREAM_NAME}" \
    --data-urlencode "definition=${STREAM_DEFINITION}" \
    --data-urlencode "deploy=false" \
    >/dev/null

  log "Deploy stream: ${STREAM_NAME}"
  curl -fsS -X POST "${DATAFLOW_URI}/streams/deployments/${STREAM_NAME}" \
    -H "Content-Type: application/json" \
    -d "${DEPLOYMENT_PROPERTIES_JSON}" \
    >/dev/null
}

main() {
  require_cmd java
  require_cmd curl
  require_cmd mvn
  require_cmd rabbitmqctl
  require_cmd lsof

  mkdir -p "$RUN_DIR" "$LOG_DIR"

  ensure_rabbitmq
  build_apps
  ensure_dataflow_jars
  start_component_if_needed "skipper" "$SKIPPER_JAR" "$SKIPPER_PORT"
  start_component_if_needed "dataflow-server" "$DATAFLOW_SERVER_JAR" "$DATAFLOW_PORT"
  wait_for_dataflow_api

  register_app "source" "client" "${LAB2_DIR}/Client/target/Client-1.0-SNAPSHOT.jar"
  register_app "processor" "comanda" "${LAB2_DIR}/Comanda/target/Comanda-1.0-SNAPSHOT.jar"
  register_app "processor" "depozit" "${LAB2_DIR}/Depozit/target/Depozit-1.0-SNAPSHOT.jar"
  register_app "processor" "facturare" "${LAB2_DIR}/Facturare/target/Facturare-1.0-SNAPSHOT.jar"
  register_app "sink" "livrare" "${LAB2_DIR}/Livrare/target/Livrare-1.0-SNAPSHOT.jar"

  prepare_stream

  log "Gata. Demo-ul este pregatit."
  log "Dashboard: ${DATAFLOW_URI}/dashboard"
  log "Loguri Data Flow: ${LOG_DIR}"
  log "Stream deployat: ${STREAM_NAME}"
}

main "$@"
