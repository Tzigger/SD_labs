#!/bin/bash
# Execuție pentru simualrea Chelneri-Bucătari
echo "================================================="
echo "   Start Simulare - Lab 5 (Chelneri & Bucatari)"
echo "================================================="

cleanup() {
    echo ""
    echo "[!] Programul de simulare a fost întrerupt de utilizator!"
    kill 0 2>/dev/null
    exit 0
}
trap cleanup EXIT INT TERM

cd "$(dirname "$0")"

echo "-> Se pornește scriptul de simulare din consolă..."
(
    VENV_PATH="../.venv"
    if [ -d "$VENV_PATH" ]; then source "$VENV_PATH/bin/activate"; fi
    python restaurant_simulation.py
)
