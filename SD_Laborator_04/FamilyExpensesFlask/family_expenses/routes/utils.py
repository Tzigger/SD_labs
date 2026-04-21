from functools import wraps

from flask import jsonify, session

from ..models import User


def get_logged_user():
    user_id = session.get("user_id")
    if user_id is None:
        return None
    return User.query.get(user_id)


def login_required(handler):
    @wraps(handler)
    def wrapper(*args, **kwargs):
        if session.get("user_id") is None:
            return jsonify({"error": "Authentication required"}), 401
        return handler(*args, **kwargs)

    return wrapper
