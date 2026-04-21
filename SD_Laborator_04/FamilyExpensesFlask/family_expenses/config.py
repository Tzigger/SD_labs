import os
from pathlib import Path


BASE_DIR = Path(__file__).resolve().parent.parent


class Config:
    SECRET_KEY = os.getenv("FLASK_SECRET_KEY", "dev-secret-change-me")
    SQLALCHEMY_DATABASE_URI = os.getenv(
        "DATABASE_URL", f"sqlite:///{BASE_DIR / 'family_expenses.db'}"
    )
    SQLALCHEMY_TRACK_MODIFICATIONS = False

    # 32-byte key encoded in Base64. Change in production.
    AES_KEY_B64 = os.getenv(
        "AES_KEY_B64", "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
    )
