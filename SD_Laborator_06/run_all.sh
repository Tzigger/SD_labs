#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$SCRIPT_DIR/logs"
CACHE_DIR="$SCRIPT_DIR/CacheMicroservice"
MERKLE_DIR="$SCRIPT_DIR/MerkleMicroservice"

mkdir -p "$LOG_DIR"

PIDS=()

setup_java_home() {
    if [[ -n "${JAVA_HOME:-}" ]]; then
        return
    fi

    local java_candidates=(
        "/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
        "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
    )

    local candidate
    for candidate in "${java_candidates[@]}"; do
        if [[ -d "$candidate" ]]; then
            export JAVA_HOME="$candidate"
            break
        fi
    done

    if [[ -z "${JAVA_HOME:-}" ]]; then
        candidate=$(ls -d /usr/local/Cellar/openjdk@21/*/libexec/openjdk.jdk/Contents/Home 2>/dev/null | head -n 1 || true)
        if [[ -z "$candidate" ]]; then
            candidate=$(ls -d /opt/homebrew/Cellar/openjdk@21/*/libexec/openjdk.jdk/Contents/Home 2>/dev/null | head -n 1 || true)
        fi
        if [[ -n "$candidate" && -d "$candidate" ]]; then
            export JAVA_HOME="$candidate"
        fi
    fi

    if [[ -n "${JAVA_HOME:-}" ]]; then
        export PATH="$JAVA_HOME/bin:$PATH"
        echo "[i] JAVA_HOME setat la: $JAVA_HOME"
    else
        echo "[warn] JAVA_HOME nu a fost setat automat. Se foloseste Java din PATH."
    fi
}

free_port() {
    local port="$1"
    local pid
    pid=$(lsof -ti :"$port" 2>/dev/null || true)
    if [[ -n "$pid" ]]; then
        echo "[i] Portul $port este ocupat. Oprire proces $pid..."
        kill -9 "$pid" 2>/dev/null || true
    fi
}

wait_for_port() {
    local port="$1"
    local service_name="$2"

    for _ in {1..60}; do
        if nc -z -w 1 localhost "$port" >/dev/null 2>&1; then
            echo "[ok] $service_name este pornit pe portul $port"
            return 0
        fi
        sleep 1
    done

    echo "[warn] $service_name nu a pornit in 60s. Verifica logul."
    return 1
}

cleanup() {
    echo ""
    echo "[i] Oprire servicii..."
    for pid in "${PIDS[@]:-}"; do
        kill "$pid" 2>/dev/null || true
    done
}

trap cleanup EXIT INT TERM

setup_java_home

if ! command -v mvn >/dev/null 2>&1; then
    echo "[err] Maven nu este instalat sau nu este in PATH."
    exit 1
fi

echo "================================================="
echo "  Start Tema Lab 6 - Cache + Merkle"
echo "================================================="

if nc -z -w 1 localhost 5672 >/dev/null 2>&1; then
    echo "[ok] RabbitMQ pare disponibil pe localhost:5672"
else
    echo "[warn] RabbitMQ NU raspunde pe localhost:5672"
    echo "[warn] Cozile printer.* nu vor functiona pana pornesti RabbitMQ"
fi

free_port 8081
free_port 8082

echo "[1/2] Pornire MerkleMicroservice..."
(
    cd "$MERKLE_DIR"
    mvn -q spring-boot:run -Dspring-boot.run.arguments=--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
) > "$LOG_DIR/merkle.log" 2>&1 &
PIDS+=("$!")
wait_for_port 8082 "MerkleMicroservice" || true

echo "[2/2] Pornire CacheMicroservice..."
(
    cd "$CACHE_DIR"
    mvn -q spring-boot:run
) > "$LOG_DIR/cache.log" 2>&1 &
PIDS+=("$!")
wait_for_port 8081 "CacheMicroservice" || true

echo ""
echo "Servicii pornite:"
echo "- Cache endpoint:  http://localhost:8081/get-cache?query=test"
echo "- Merkle endpoint: http://localhost:8082/search-zone?hash=<hash>"
echo ""
echo "Loguri:"
echo "- $LOG_DIR/cache.log"
echo "- $LOG_DIR/merkle.log"
echo ""
echo "Apasa Ctrl+C pentru oprire."

wait
