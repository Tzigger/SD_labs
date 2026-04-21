# Family Expenses - Service and Class Design

## Service diagram

```mermaid
flowchart LR
    Browser[Flask HTML UI / API Client] --> AuthAPI[/Auth REST Service/]
    Browser --> ExpensesAPI[/Expenses REST Service/]
    Browser --> CryptoAPI[/AES Crypto REST Service/]

    AuthAPI --> AuthService[Password Hash Service]
    AuthAPI --> CryptoDomain[AES Encryption Service]
    AuthAPI --> UserRepo[(users table)]

    ExpensesAPI --> ExpenseRepo[(expenses table)]
    ExpensesAPI --> AuthSession[Session Authentication]

    CryptoAPI --> CryptoDomain

    CryptoDomain --> UserRepo
```

## Class diagram

```mermaid
classDiagram
    class User {
        +int id
        +string username
        +string password_hash
        +string first_name_encrypted
        +string last_name_encrypted
        +datetime created_at
    }

    class Expense {
        +int id
        +int user_id
        +string category
        +float amount
        +string description
        +date spent_on
        +datetime created_at
        +to_dict() dict
    }

    class CryptoService {
        -AESGCM _aesgcm
        +initialize(base64_key)
        +encrypt_text(plain_text) str
        +decrypt_text(encrypted_text) str
    }

    class AuthService {
        +password_hash(username, password) str
        +verify_password(username, plain_password, stored_hash) bool
    }

    class AuthApiController {
        +register_user()
        +login_user()
        +logout_user()
        +get_profile()
    }

    class ExpensesApiController {
        +list_expenses()
        +create_expense()
        +update_expense(expense_id)
        +delete_expense(expense_id)
    }

    class CryptoApiController {
        +encrypt_text()
        +decrypt_text()
    }

    User "1" --> "0..*" Expense : owns
    AuthApiController --> AuthService : uses
    AuthApiController --> CryptoService : encrypt/decrypt names
    AuthApiController --> User : CRUD login data
    ExpensesApiController --> Expense : CRUD
    CryptoApiController --> CryptoService : uses
```

## Requirement mapping

- RESTful services for family expenses:
  - `GET/POST /api/expenses`
  - `PUT/DELETE /api/expenses/<id>`
- Each member has account with username + password:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
- Categories are restricted to:
  - `intretinere`, `mancare`, `distractie`, `scoala`, `personale`
- AES service:
  - `POST /api/crypto/encrypt`
  - `POST /api/crypto/decrypt`
- Personal data encryption before DB save:
  - `first_name` and `last_name` are AES encrypted before insert in `users` table.
- Password hashing rule:
  - `sha256(username + ":" + password)`
- Flask interface:
  - `/` serves HTML that calls all REST services with JavaScript `fetch`.
