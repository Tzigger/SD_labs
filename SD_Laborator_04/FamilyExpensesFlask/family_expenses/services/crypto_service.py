import base64
import os

from cryptography.hazmat.primitives.ciphers.aead import AESGCM


class CryptoService:
    def __init__(self) -> None:
        self._aesgcm = None

    def initialize(self, base64_key: str) -> None:
        key = base64.b64decode(base64_key)
        if len(key) not in (16, 24, 32):
            raise ValueError("AES key must have 16, 24 or 32 bytes")
        self._aesgcm = AESGCM(key)

    def encrypt_text(self, plain_text: str) -> str:
        if self._aesgcm is None:
            raise RuntimeError("Crypto service is not initialized")
        nonce = os.urandom(12)
        cipher = self._aesgcm.encrypt(nonce, plain_text.encode("utf-8"), None)
        return base64.urlsafe_b64encode(nonce + cipher).decode("utf-8")

    def decrypt_text(self, encrypted_text: str) -> str:
        if self._aesgcm is None:
            raise RuntimeError("Crypto service is not initialized")
        payload = base64.urlsafe_b64decode(encrypted_text.encode("utf-8"))
        nonce = payload[:12]
        cipher = payload[12:]
        plain = self._aesgcm.decrypt(nonce, cipher, None)
        return plain.decode("utf-8")


crypto_service = CryptoService()
