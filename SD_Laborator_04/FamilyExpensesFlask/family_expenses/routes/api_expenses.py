from datetime import date

from flask import Blueprint, jsonify, request

from ..models import EXPENSE_CATEGORIES, Expense, db
from .utils import get_logged_user, login_required


expenses_bp = Blueprint("expenses_bp", __name__, url_prefix="/api/expenses")


def _validate_expense_payload(data):
    category = (data.get("category") or "").strip().lower()
    amount_value = data.get("amount")
    description = (data.get("description") or "").strip()
    spent_on_str = (data.get("spent_on") or "").strip()

    if category not in EXPENSE_CATEGORIES:
        return None, "Invalid category"

    try:
        amount = float(amount_value)
        if amount <= 0:
            return None, "Amount must be > 0"
    except (TypeError, ValueError):
        return None, "Amount must be a valid number"

    try:
        spent_on = date.fromisoformat(spent_on_str)
    except ValueError:
        return None, "spent_on must be in YYYY-MM-DD format"

    return {
        "category": category,
        "amount": amount,
        "description": description,
        "spent_on": spent_on,
    }, None


@expenses_bp.route("", methods=["GET"])
@login_required
def list_expenses():
    user = get_logged_user()
    items = Expense.query.filter_by(user_id=user.id).order_by(Expense.spent_on.desc()).all()
    return jsonify([item.to_dict() for item in items]), 200


@expenses_bp.route("", methods=["POST"])
@login_required
def create_expense():
    user = get_logged_user()
    data = request.get_json(silent=True) or {}

    validated, error = _validate_expense_payload(data)
    if error is not None:
        return jsonify({"error": error}), 400

    expense = Expense(
        user_id=user.id,
        category=validated["category"],
        amount=validated["amount"],
        description=validated["description"],
        spent_on=validated["spent_on"],
    )

    db.session.add(expense)
    db.session.commit()

    return jsonify(expense.to_dict()), 201


@expenses_bp.route("/<int:expense_id>", methods=["PUT"])
@login_required
def update_expense(expense_id: int):
    user = get_logged_user()
    expense = Expense.query.filter_by(id=expense_id, user_id=user.id).first()
    if expense is None:
        return jsonify({"error": "Expense not found"}), 404

    data = request.get_json(silent=True) or {}
    validated, error = _validate_expense_payload(data)
    if error is not None:
        return jsonify({"error": error}), 400

    expense.category = validated["category"]
    expense.amount = validated["amount"]
    expense.description = validated["description"]
    expense.spent_on = validated["spent_on"]

    db.session.commit()
    return jsonify(expense.to_dict()), 200


@expenses_bp.route("/<int:expense_id>", methods=["DELETE"])
@login_required
def delete_expense(expense_id: int):
    user = get_logged_user()
    expense = Expense.query.filter_by(id=expense_id, user_id=user.id).first()
    if expense is None:
        return jsonify({"error": "Expense not found"}), 404

    db.session.delete(expense)
    db.session.commit()
    return jsonify({"message": "Expense deleted"}), 200
