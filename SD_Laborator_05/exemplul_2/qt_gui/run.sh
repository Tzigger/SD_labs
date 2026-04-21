#!/bin/bash
echo "Pornire Qt GUI (Exemplul 2)..."

cd "$(dirname "$0")"

VENV_PATH="../../.venv"
if [ -d "$VENV_PATH" ]; then
    echo "Se activează virtual environment..."
    source "$VENV_PATH/bin/activate"
fi

pip install -r requirements.txt 2>/dev/null || true
python exemplul_2.py
