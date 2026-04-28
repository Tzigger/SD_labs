#!/bin/bash
echo "Pornire LibraryApp..."

cleanup() {
    echo ""
    echo "[!] Se caută eliberarea portului 8080..."
    PID=$(lsof -t -i:8080 2>/dev/null)
    if [ ! -z "$PID" ]; then
        kill -9 $PID 2>/dev/null
        echo "Portul 8080 a fost eliberat - $PID"
    else
        echo "[✓] Portul 8080 este deja liber."
    fi
}
trap cleanup EXIT INT TERM

cd "$(dirname "$0")"
if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    JAVA_21_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || true)
    if [ ! -z "$JAVA_21_HOME" ]; then export JAVA_HOME="$JAVA_21_HOME"; fi
fi
if [ -d "/opt/homebrew/opt/openjdk@21/bin" ]; then export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"; fi
if [ -d "/usr/local/opt/openjdk@21/bin" ]; then export PATH="/usr/local/opt/openjdk@21/bin:$PATH"; fi
mvn clean spring-boot:run
