#!/bin/bash
# Execuție integrală pentru Exemplul 1
echo "================================================="
echo "   Start Mediu de Testare - Lab 5 (Exemplul 1)"
echo "   Atenție: Ai nevoie de RabbitMQ pornit local"
echo "================================================="

# Funcție generală de curățare a proceselor lansate
cleanup() {
    echo ""
    echo "[!] Se face Clean-Up. Se omoară procesele Java și fereastra de Python..."
    # kill 0 trimite SIGTERM către tot grupul de procese generat de acest script
    kill 0 2>/dev/null
    exit 0
}

# Agățăm interceptarea
trap cleanup EXIT INT TERM

cd "$(dirname "$0")"

# Eliberăm portul
PID_PORT=$(lsof -t -i:8080 2>/dev/null)
if [ ! -z "$PID_PORT" ]; then kill -9 $PID_PORT 2>/dev/null; fi

echo "-> 1. Pornire StackApp Backend (rulare in fundal)..."
(
    if command -v /usr/libexec/java_home >/dev/null 2>&1; then
        JAVA_21_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || true)
        if [ ! -z "$JAVA_21_HOME" ]; then export JAVA_HOME="$JAVA_21_HOME"; fi
    fi
    if [ -d "/opt/homebrew/opt/openjdk@21/bin" ]; then export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"; fi
    if [ -d "/usr/local/opt/openjdk@21/bin" ]; then export PATH="/usr/local/opt/openjdk@21/bin:$PATH"; fi
    cd StackApp && mvn clean spring-boot:run
) &

# Așteptăm inițializarea Spring Boot
echo "-> Așteptăm 15 secunde să pornească complet serverul Java și AMQP..."
sleep 15

echo "-> 2. Pornire Interfață Client (Qt GUI)..."
(
    cd qt_gui
    VENV_PATH="../../.venv"
    if [ -d "$VENV_PATH" ]; then source "$VENV_PATH/bin/activate"; fi
    python exemplul_1_v1.py
)

# Note: Când fereastra GUI este închisă normal, script-ul se continuă și apelează cleanup.
