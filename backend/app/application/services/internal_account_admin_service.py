from datetime import datetime, timezone

from fastapi import HTTPException, status

from app.application.schemas.auth_schemas import AccountResponse
from app.application.schemas.internal_account_schemas import (
    CreateInternalAccountRequest,
    UpdateInternalAccountRequest,
)
from app.core.auth_scope import build_scoped_username, normalize_client_variant
from app.core.config import settings
from app.core.id_generator import generate_public_id
from app.core.security import hash_password
from app.infrastructure.db.models.auth import AuthAccountModel


class InternalAccountAdminService:
    def __init__(self, db):
        self._db = db
        self._variant = normalize_client_variant(settings.internal_auth_variant)

    def list_accounts(self) -> list[AccountResponse]:
        rows = (
            self._db.query(AuthAccountModel)
            .filter(AuthAccountModel.client_variant == self._variant)
            .order_by(AuthAccountModel.created_at.desc())
            .all()
        )
        return [self._to_response(row) for row in rows]

    def create_account(self, payload: CreateInternalAccountRequest) -> AccountResponse:
        username = payload.username.strip()
        scoped_username = build_scoped_username(username, self._variant)
        existing = (
            self._db.query(AuthAccountModel)
            .filter(
                AuthAccountModel.username == scoped_username,
                AuthAccountModel.client_variant == self._variant,
            )
            .first()
        )
        if existing is not None:
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="内部账号已存在。")

        now = self._now_millis()
        account = AuthAccountModel(
            id=generate_public_id("acct"),
            username=scoped_username,
            display_username=username,
            client_variant=self._variant,
            remark=payload.remark.strip(),
            password_hash=hash_password(payload.password),
            status="active",
            bound_device_id="",
            bound_device_name="",
            failed_device_attempts=0,
            last_login_at=0,
            created_at=now,
            updated_at=now,
        )
        self._db.add(account)
        self._db.commit()
        self._db.refresh(account)
        return self._to_response(account)

    def update_account(self, account_id: str, payload: UpdateInternalAccountRequest) -> AccountResponse:
        account = self._find_account(account_id)
        if account is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="内部账号不存在。")

        normalized_status = (payload.status or "active").strip().lower()
        if normalized_status not in {"active", "banned"}:
            normalized_status = "active"

        account.remark = (payload.remark or "").strip()
        account.status = normalized_status
        if payload.password.strip():
            account.password_hash = hash_password(payload.password.strip())
        if payload.resetBoundDevice:
            account.bound_device_id = ""
            account.bound_device_name = ""
            account.failed_device_attempts = 0
        account.updated_at = self._now_millis()
        self._db.commit()
        self._db.refresh(account)
        return self._to_response(account)

    def delete_account(self, account_id: str) -> None:
        account = self._find_account(account_id)
        if account is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="内部账号不存在。")
        self._db.delete(account)
        self._db.commit()

    def _find_account(self, account_id: str) -> AuthAccountModel | None:
        return (
            self._db.query(AuthAccountModel)
            .filter(
                AuthAccountModel.id == account_id,
                AuthAccountModel.client_variant == self._variant,
            )
            .first()
        )

    def _to_response(self, account: AuthAccountModel) -> AccountResponse:
        return AccountResponse(
            id=account.id,
            username=account.display_username or account.username,
            clientVariant=account.client_variant or self._variant,
            remark=account.remark or "",
            status=account.status,
            boundDeviceId=account.bound_device_id or "",
            boundDeviceName=account.bound_device_name or "",
            failedDeviceAttempts=account.failed_device_attempts or 0,
            lastLoginAt=account.last_login_at or 0,
            createdAt=account.created_at,
            updatedAt=account.updated_at,
        )

    def _now_millis(self) -> int:
        return int(datetime.now(timezone.utc).timestamp() * 1000)
