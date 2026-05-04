#!/usr/bin/env bash

set -euo pipefail

DATAFLOW_URI="${DATAFLOW_URI:-http://localhost:9393}"
STREAM_NAME="${STREAM_NAME:-lab9demo}"
TAIL_LINES="${TAIL_LINES:-5}"
WATCH_INTERVAL="${WATCH_INTERVAL:-0}"

SCRIPT_NAME="$(basename "$0")"

usage() {
  cat <<EOF
Utilizare:
  $SCRIPT_NAME                 # afiseaza snapshot-ul curent
  $SCRIPT_NAME --watch         # refresh continuu (la 2 sec)
  $SCRIPT_NAME --watch=3       # refresh continuu (la 3 sec)
EOF
}

is_port_open() {
  local port="$1"
  lsof -nP -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1
}

service_status() {
  local name="$1"
  local port="$2"
  if is_port_open "$port"; then
    echo "UP"
  else
    echo "DOWN"
  fi
}

java_tmpdir() {
  java -XshowSettings:properties -version 2>&1 \
    | awk -F= '/java.io.tmpdir/{gsub(/^[ \t]+/, "", $2); print $2; exit}'
}

print_separator() {
  printf '%s\n' "------------------------------------------------------------"
}

print_file_tail() {
  local file="$1"
  local lines="$2"

  if [[ -f "$file" ]]; then
    tail -n "$lines" "$file"
  else
    echo "(fisier lipsa: $file)"
  fi
}

find_runtime_log() {
  local tmpdir="$1"
  local app="$2"
  local file
  local best_file=""
  local best_version=-1
  local prefix="${STREAM_NAME}.${app}-v"
  local rest
  local version

  while IFS= read -r -d '' file; do
    if [[ "$file" == *"$prefix"*"/stdout_0.log" ]]; then
      rest="${file#*${prefix}}"
      version="${rest%%/*}"
      if [[ "$version" =~ ^[0-9]+$ ]] && (( version > best_version )); then
        best_version="$version"
        best_file="$file"
      fi
    fi
  done < <(
    find "${tmpdir%/}" -maxdepth 3 -type f -path "*/${STREAM_NAME}.${app}-v*/stdout_0.log" -print0 2>/dev/null || true
  )

  printf '%s' "$best_file"
}

print_runtime_log_tail() {
  local tmpdir="$1"
  local app="$2"
  local log_file

  log_file="$(find_runtime_log "$tmpdir" "$app")"

  if [[ -n "$log_file" && -f "$log_file" ]]; then
    tail -n 2 "$log_file"
  else
    echo "(fara log runtime pentru ${app})"
  fi
}

print_dataflow_snapshot() {
  local endpoint="$1"
  local response
  local http_code
  local body
  local url="${DATAFLOW_URI}/${endpoint}"

  response="$(curl -sS -m 3 -w $'\n__HTTP_STATUS__:%{http_code}' "$url" 2>/dev/null || true)"
  http_code="$(printf '%s\n' "$response" | sed -n 's/^__HTTP_STATUS__://p' | tail -n 1)"
  body="$(printf '%s\n' "$response" | sed '/^__HTTP_STATUS__:/d')"

  if [[ -z "$http_code" || "$http_code" == "000" ]]; then
    echo "(indisponibil: $url)"
    return 0
  fi

  echo "HTTP ${http_code}"
  if [[ -n "$body" ]]; then
    echo "$body" | sed -e 's/[[:space:]]\+/ /g' | cut -c1-220
  fi
}

render_once() {
  local tmpdir
  local db_dir

  tmpdir="$(java_tmpdir)"
  db_dir="${tmpdir%/}/sd_lab09_db"

  print_separator
  echo "LAB 9 Demo Dashboard"
  echo "Timp: $(date '+%Y-%m-%d %H:%M:%S')"
  echo "Data Flow UI: ${DATAFLOW_URI}/dashboard"
  echo "DB folder: ${db_dir}"

  print_separator
  echo "Servicii:"
  echo "RabbitMQ 5672   : $(service_status "rabbitmq" 5672)"
  echo "Skipper 7577    : $(service_status "skipper" 7577)"
  echo "Data Flow 9393  : $(service_status "dataflow" 9393)"

  print_separator
  echo "Data Flow stream definition (${STREAM_NAME}):"
  print_dataflow_snapshot "streams/definitions/${STREAM_NAME}"

  print_separator
  echo "Data Flow runtime stream (${STREAM_NAME}):"
  print_dataflow_snapshot "runtime/streams/${STREAM_NAME}"

  print_separator
  echo "Ultimele ${TAIL_LINES} linii din fisiere:"
  echo "[clienti.txt]"
  print_file_tail "${db_dir}/clienti.txt" "$TAIL_LINES"
  echo
  echo "[comenzi.txt]"
  print_file_tail "${db_dir}/comenzi.txt" "$TAIL_LINES"
  echo
  echo "[facturi.txt]"
  print_file_tail "${db_dir}/facturi.txt" "$TAIL_LINES"
  echo
  echo "[stocuri.txt]"
  print_file_tail "${db_dir}/stocuri.txt" "$TAIL_LINES"

  print_separator
  echo "Ultimele loguri runtime (2 linii / app):"
  echo "[client]"
  print_runtime_log_tail "$tmpdir" "client"
  echo "[comanda]"
  print_runtime_log_tail "$tmpdir" "comanda"
  echo "[depozit]"
  print_runtime_log_tail "$tmpdir" "depozit"
  echo "[facturare]"
  print_runtime_log_tail "$tmpdir" "facturare"
  echo "[livrare]"
  print_runtime_log_tail "$tmpdir" "livrare"
}

parse_args() {
  if [[ $# -eq 0 ]]; then
    return 0
  fi

  case "$1" in
    --help|-h)
      usage
      exit 0
      ;;
    --watch)
      WATCH_INTERVAL=2
      ;;
    --watch=*)
      WATCH_INTERVAL="${1#*=}"
      ;;
    *)
      echo "Argument necunoscut: $1" >&2
      usage
      exit 1
      ;;
  esac
}

main() {
  parse_args "$@"

  if [[ "${WATCH_INTERVAL}" -gt 0 ]]; then
    while true; do
      clear
      render_once
      sleep "${WATCH_INTERVAL}"
    done
  else
    render_once
  fi
}

main "$@"
