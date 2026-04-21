#!/bin/bash
# Script de rulare pentru WeatherAppOrchestration
echo "Pornire WeatherAppOrchestration..."

# Funcție pentru forțarea eliberării portului 8080
cleanup() {
    echo ""
    echo "[!] Se închide aplicația și se eliberează portul 8080..."
    # Găsește orice proces care ascultă pe 8080 și îl omoară
    PID=$(lsof -t -i:8080 2>/dev/null)
    if [ ! -z "$PID" ]; then
        kill -9 $PID 2>/dev/null
        echo "Portul 8080 a fost eliberat - $PID"
    else
        echo "[✓] Portul 8080 este deja liber."
    fi
}

# Captăm semnalul de întrerupere (Ctrl+C)
trap cleanup EXIT INT TERM

# Mută directorul curent la locația scriptului
cd "$(dirname "$0")"

# Rularea aplicației folosind Maven wrapper
./mvnw clean spring-boot:run
