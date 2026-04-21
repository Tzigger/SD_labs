import hashlib


def password_hash(username: str, password: str) -> str:
    seed = f"{username}:{password}".encode("utf-8")
    return hashlib.sha256(seed).hexdigest()


def verify_password(username: str, plain_password: str, stored_hash: str) -> bool:
    return password_hash(username, plain_password) == stored_hash
