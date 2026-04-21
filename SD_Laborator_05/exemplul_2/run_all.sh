#!/bin/bash
# Execuție integrală pentru Exemplul 2
echo "================================================="
echo "   Start Mediu de Testare - Lab 5 (Exemplul 2)"
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

echo "-> 1. Pornire LibraryApp Backend (rulare in fundal)..."
(
    export JAVA_HOME=/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
    export PATH="$JAVA_HOME/bin:$PATH"
    cd LibraryApp && mvn clean spring-boot:run
) &

# Așteptăm inițializarea Spring Boot
echo "-> Așteptăm 15 secunde să pornească complet serverul Java și AMQP..."
sleep 15

echo "-> 2. Pornire Interfață Client (Qt GUI)..."
(
    cd qt_gui
    VENV_PATH="../../.venv"
    if [ -d "$VENV_PATH" ]; then source "$VENV_PATH/bin/activate"; fi
    python exemplul_2.py
)

# Note: Când fereastra GUI este închisă normal, script-ul se continuă și apelează cleanup.
