from flask import Flask

from .config import Config
from .models import db
from .routes.api_auth import auth_bp
from .routes.api_crypto import crypto_bp
from .routes.api_expenses import expenses_bp
from .routes.web import web_bp
from .services.crypto_service import crypto_service


def create_app() -> Flask:
    app = Flask(__name__)
    app.config.from_object(Config)

    db.init_app(app)
    crypto_service.initialize(app.config["AES_KEY_B64"])

    app.register_blueprint(web_bp)
    app.register_blueprint(auth_bp)
    app.register_blueprint(expenses_bp)
    app.register_blueprint(crypto_bp)

    return app
