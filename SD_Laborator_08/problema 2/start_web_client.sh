#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WEB_DIR="$SCRIPT_DIR/WebClient"

WITH_LAB=0
LAB_BUILD_MODE="--no-build"
WEB_PORT="${WEB_CLIENT_PORT:-3000}"

usage() {
  cat <<'EOF'
Usage: ./start_web_client.sh [options]

Options:
  --with-lab   Porneste mai intai laboratorul Docker (apeleaza start_lab.sh).
  --build-lab  Folosit impreuna cu --with-lab, face rebuild imagini.
  --port N     Portul HTTP pentru clientul web (default: 3000).
  -h, --help   Afiseaza acest mesaj.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --with-lab)
      WITH_LAB=1
      shift
      ;;
    --build-lab)
      LAB_BUILD_MODE=""
      shift
      ;;
    --port)
      if [[ $# -lt 2 ]]; then
        echo "--port necesita un argument." >&2
        exit 1
      fi
      WEB_PORT="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Optiune necunoscuta: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if ! command -v node >/dev/null 2>&1; then
  echo "Node.js nu este instalat sau nu este in PATH." >&2
  exit 1
fi

if [[ "$WITH_LAB" -eq 1 ]]; then
  echo "Pornesc laboratorul Docker..."
  if [[ -z "$LAB_BUILD_MODE" ]]; then
    "$SCRIPT_DIR/start_lab.sh"
  else
    "$SCRIPT_DIR/start_lab.sh" "$LAB_BUILD_MODE"
  fi
fi

echo "Pornesc Web Client pe http://127.0.0.1:$WEB_PORT"
cd "$WEB_DIR"
WEB_CLIENT_PORT="$WEB_PORT" npm start
