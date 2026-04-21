#!/bin/bash
echo "Pornire StackApp..."

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
export JAVA_HOME=/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn clean spring-boot:run
