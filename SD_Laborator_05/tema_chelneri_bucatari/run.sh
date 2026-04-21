#!/bin/bash
echo "Pornire Simulare Restaurant Chelneri-Bucătari..."

cd "$(dirname "$0")"

VENV_PATH="../.venv"
if [ -d "$VENV_PATH" ]; then
    echo "Se activează virtual environment..."
    source "$VENV_PATH/bin/activate"
fi

python restaurant_simulation.py
