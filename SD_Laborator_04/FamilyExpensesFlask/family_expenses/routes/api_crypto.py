from flask import Blueprint, jsonify, request

from ..services.crypto_service import crypto_service


crypto_bp = Blueprint("crypto_bp", __name__, url_prefix="/api/crypto")


@crypto_bp.route("/encrypt", methods=["POST"])
def encrypt_text():
    data = request.get_json(silent=True) or {}
    plain_text = data.get("text") or ""

    if not plain_text:
        return jsonify({"error": "text is required"}), 400

    encrypted = crypto_service.encrypt_text(plain_text)
    return jsonify({"encrypted": encrypted}), 200


@crypto_bp.route("/decrypt", methods=["POST"])
def decrypt_text():
    data = request.get_json(silent=True) or {}
    encrypted = data.get("encrypted") or ""

    if not encrypted:
        return jsonify({"error": "encrypted is required"}), 400

    try:
        plain_text = crypto_service.decrypt_text(encrypted)
    except Exception:
        return jsonify({"error": "Invalid encrypted payload"}), 400

    return jsonify({"text": plain_text}), 200
