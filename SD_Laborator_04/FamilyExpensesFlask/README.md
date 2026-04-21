# FamilyExpensesFlask

RESTful application for managing family expenses, with:
- account registration/login (username + password)
- password hash based on `username:password`
- AES encryption service for personal data
- Flask web interface for testing all operations

## Features

- Auth:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `POST /api/auth/logout`
  - `GET /api/auth/me`
- Expenses:
  - `GET /api/expenses`
  - `POST /api/expenses`
  - `PUT /api/expenses/<id>`
  - `DELETE /api/expenses/<id>`
- Crypto:
  - `POST /api/crypto/encrypt`
  - `POST /api/crypto/decrypt`

Allowed categories:
- `intretinere`
- `mancare`
- `distractie`
- `scoala`
- `personale`

## Run locally

1. Create virtual environment:

```bash
python3 -m venv .venv
source .venv/bin/activate
```

2. Install dependencies:

```bash
pip install -r requirements.txt
```

3. Start the app:

```bash
python app.py
```

4. Open browser:

- `http://127.0.0.1:5000/`

## Notes

- SQLite database file is created automatically: `family_expenses.db`.
- In this educational implementation, AES key and Flask secret have default values and should be changed for production.
- Architecture diagrams are available in `DESIGN.md`.
