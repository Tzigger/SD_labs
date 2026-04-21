from flask import Blueprint, jsonify, request, session

from ..models import User, db
from ..services.auth_service import password_hash, verify_password
from ..services.crypto_service import crypto_service
from .utils import get_logged_user, login_required


auth_bp = Blueprint("auth_bp", __name__, url_prefix="/api/auth")


@auth_bp.route("/register", methods=["POST"])
def register_user():
    data = request.get_json(silent=True) or {}

    username = (data.get("username") or "").strip()
    password = data.get("password") or ""
    first_name = (data.get("first_name") or "").strip()
    last_name = (data.get("last_name") or "").strip()

    if not username or not password or not first_name or not last_name:
        return jsonify({"error": "username, password, first_name and last_name are required"}), 400

    if User.query.filter_by(username=username).first() is not None:
        return jsonify({"error": "Username already exists"}), 409

    user = User(
        username=username,
        password_hash=password_hash(username, password),
        first_name_encrypted=crypto_service.encrypt_text(first_name),
        last_name_encrypted=crypto_service.encrypt_text(last_name),
    )

    db.session.add(user)
    db.session.commit()

    return jsonify({"id": user.id, "username": user.username}), 201


@auth_bp.route("/login", methods=["POST"])
def login_user():
    data = request.get_json(silent=True) or {}

    username = (data.get("username") or "").strip()
    password = data.get("password") or ""

    user = User.query.filter_by(username=username).first()
    if user is None or not verify_password(username, password, user.password_hash):
        return jsonify({"error": "Invalid credentials"}), 401

    session["user_id"] = user.id
    return jsonify({"message": "Login successful", "user_id": user.id}), 200


@auth_bp.route("/logout", methods=["POST"])
@login_required
def logout_user():
    session.pop("user_id", None)
    return jsonify({"message": "Logout successful"}), 200


@auth_bp.route("/me", methods=["GET"])
@login_required
def get_profile():
    user = get_logged_user()
    if user is None:
        return jsonify({"error": "User not found"}), 404

    return jsonify(
        {
            "id": user.id,
            "username": user.username,
            "first_name": crypto_service.decrypt_text(user.first_name_encrypted),
            "last_name": crypto_service.decrypt_text(user.last_name_encrypted),
        }
    )
