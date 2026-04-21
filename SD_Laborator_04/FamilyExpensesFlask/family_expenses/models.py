from datetime import datetime

from flask_sqlalchemy import SQLAlchemy


db = SQLAlchemy()

EXPENSE_CATEGORIES = (
    "intretinere",
    "mancare",
    "distractie",
    "scoala",
    "personale",
)


class User(db.Model):
    __tablename__ = "users"

    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False)
    password_hash = db.Column(db.String(128), nullable=False)
    first_name_encrypted = db.Column(db.Text, nullable=False)
    last_name_encrypted = db.Column(db.Text, nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow, nullable=False)

    expenses = db.relationship("Expense", backref="user", lazy=True, cascade="all, delete-orphan")


class Expense(db.Model):
    __tablename__ = "expenses"

    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)
    category = db.Column(db.String(30), nullable=False)
    amount = db.Column(db.Float, nullable=False)
    description = db.Column(db.String(255), nullable=True)
    spent_on = db.Column(db.Date, nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow, nullable=False)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "user_id": self.user_id,
            "category": self.category,
            "amount": self.amount,
            "description": self.description,
            "spent_on": self.spent_on.isoformat(),
            "created_at": self.created_at.isoformat(),
        }
