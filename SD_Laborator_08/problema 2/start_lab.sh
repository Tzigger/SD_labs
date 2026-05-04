#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

SERVICES=(
  message_manager
  teacher_microservice
  student_microservice_1
  student_microservice_2
  student_microservice_3
)

BUILD_MODE="--build"
RUN_CLIENT=0
FOLLOW_LOGS=0

usage() {
  cat <<'EOF'
Usage: ./start_lab.sh [options]

Options:
  --no-build   Porneste stack-ul fara rebuild de imagini.
  --client     Porneste clientul web dupa ce serviciile sunt up.
  --logs       Afiseaza log-urile dupa pornire (follow).
  -h, --help   Afiseaza acest mesaj.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-build)
      BUILD_MODE=""
      shift
      ;;
    --client)
      RUN_CLIENT=1
      shift
      ;;
    --logs)
      FOLLOW_LOGS=1
      shift
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

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker nu este instalat sau nu este in PATH." >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker daemon nu ruleaza. Porneste Docker Desktop si reincearca." >&2
  exit 1
fi

echo "Pornesc laboratorul (Docker Compose)..."
if [[ -n "$BUILD_MODE" ]]; then
  docker compose up --build -d
else
  docker compose up -d
fi

echo "Astept pornirea serviciilor..."
READY=0
# Wait-until-ready: consideram laboratorul pornit doar cand toate serviciile
# necesare pentru demo sunt in starea "running".
for _ in $(seq 1 25); do
  RUNNING_SERVICES="$(docker compose ps --status running --services || true)"
  ALL_UP=1

  for SERVICE in "${SERVICES[@]}"; do
    if ! echo "$RUNNING_SERVICES" | grep -qx "$SERVICE"; then
      ALL_UP=0
      break
    fi
  done

  if [[ "$ALL_UP" -eq 1 ]]; then
    READY=1
    break
  fi

  sleep 1
done

if [[ "$READY" -ne 1 ]]; then
  echo "Unele servicii nu au ajuns in starea running in timp util." >&2
  docker compose ps
  exit 1
fi

echo
echo "Laborator pornit. Stare containere:"
docker compose ps
echo
echo "Comenzi utile:"
echo "  docker compose logs --tail=50"
echo "  docker compose down"
echo

if [[ "$RUN_CLIENT" -eq 1 ]]; then
  echo "Pornesc clientul web..."
  "$SCRIPT_DIR/start_web_client.sh"
fi

if [[ "$FOLLOW_LOGS" -eq 1 ]]; then
  echo "Afisez log-uri live (CTRL+C pentru stop logs)."
  docker compose logs -f
fi
