#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$SCRIPT_DIR/../logs"
mkdir -p "$LOG_DIR"

if [[ -z "${JAVA_HOME:-}" ]]; then
    if [[ -d "/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" ]]; then
        export JAVA_HOME="/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
    elif [[ -d "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" ]]; then
        export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
    else
        CANDIDATE=$(ls -d /usr/local/Cellar/openjdk@21/*/libexec/openjdk.jdk/Contents/Home 2>/dev/null | head -n 1 || true)
        if [[ -z "$CANDIDATE" ]]; then
            CANDIDATE=$(ls -d /opt/homebrew/Cellar/openjdk@21/*/libexec/openjdk.jdk/Contents/Home 2>/dev/null | head -n 1 || true)
        fi
        if [[ -n "$CANDIDATE" && -d "$CANDIDATE" ]]; then
            export JAVA_HOME="$CANDIDATE"
        fi
    fi
fi

if [[ -n "${JAVA_HOME:-}" ]]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

PID_PORT=$(lsof -ti :8082 2>/dev/null || true)
if [[ -n "$PID_PORT" ]]; then
    kill -9 "$PID_PORT" 2>/dev/null || true
fi

echo "Pornire MerkleMicroservice pe portul 8082..."
cd "$SCRIPT_DIR"
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration | tee "$LOG_DIR/merkle.log"
