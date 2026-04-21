#!/bin/bash
# Script de rulare pentru FamilyExpensesFlask
echo "Pornire FamilyExpensesFlask..."

cleanup() {
    echo ""
    echo "[!] Se închide aplicația și se eliberează portul 5000..."
    PID=$(lsof -t -i:5000 2>/dev/null)
    if [ ! -z "$PID" ]; then
        kill -9 $PID 2>/dev/null
        echo "Portul 5000 a fost eliberat - $PID"
    else
        echo "[✓] Portul 5000 este deja liber."
    fi
}

trap cleanup EXIT INT TERM

cd "$(dirname "$0")"

# Activare venv dacă există, altfel folosește python/pip global
VENV_PATH="../.venv"
if [ -d "$VENV_PATH" ]; then
    echo "Se activează virtual environment din $VENV_PATH..."
    source "$VENV_PATH/bin/activate"
fi

# Instalare dependențe dacă nu sunt deja
pip install -r requirements.txt 2>/dev/null || true

# Rularea aplicației Python
python app.py
