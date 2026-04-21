#!/bin/bash
# Script de rulare pentru PasswordEncryptionExample
echo "Pornire PasswordEncryptionExample..."

cleanup() {
    echo ""
    echo "[!] Se închide aplicația și se eliberează portul 2030..."
    PID=$(lsof -t -i:2030 2>/dev/null)
    if [ ! -z "$PID" ]; then
        kill -9 $PID 2>/dev/null
        echo "Portul 2030 a fost eliberat - $PID"
    else
        echo "[✓] Portul 2030 este deja liber."
    fi
}

trap cleanup EXIT INT TERM

cd "$(dirname "$0")"

# Rularea aplicației folosind Maven
export JAVA_HOME=/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn clean spring-boot:run
