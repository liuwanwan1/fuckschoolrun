from datetime import datetime, timezone

from app.application.schemas.notice_schemas import (
    AppNoticeResponse,
    RootAccessPolicyResponse,
    UpdateAppNoticeRequest,
    UpdateRootAccessPolicyRequest,
)
from app.core.config import settings
from app.core.root_access_policy import (
    DEFAULT_ROOT_ACCESS_TYPES,
    normalize_root_access_types,
    parse_root_access_types,
    serialize_root_access_types,
)
from app.infrastructure.db.models.app_notice import AppNoticeModel


class AppNoticeService:
    DEFAULT_NOTICE_ID = "default"

    def __init__(self, db):
        self._db = db

    def get_notice(self) -> AppNoticeResponse:
        notice = self._get_or_create_notice()
        return self._to_response(notice)

    def get_root_access_policy(self) -> RootAccessPolicyResponse:
        notice = self._get_or_create_notice()
        return self._to_root_access_policy_response(notice)

    def update_notice(self, payload: UpdateAppNoticeRequest) -> AppNoticeResponse:
        notice = self._get_or_create_notice()
        notice.title = payload.title.strip()
        notice.message = payload.message.strip()
        notice.qq_group_number = payload.qqGroupNumber.strip()
        notice.bilibili_text = payload.bilibiliText.strip()
        notice.bilibili_url = payload.bilibiliUrl.strip()
        if payload.rootAccessAllowedTesterTypes is not None:
            notice.root_access_allowed_tester_types = serialize_root_access_types(payload.rootAccessAllowedTesterTypes)
        notice.updated_at = self._now_millis()
        self._db.commit()
        self._db.refresh(notice)
        return self._to_response(notice)

    def update_root_access_policy(self, payload: UpdateRootAccessPolicyRequest) -> RootAccessPolicyResponse:
        notice = self._get_or_create_notice()
        notice.root_access_allowed_tester_types = serialize_root_access_types(payload.rootAccessAllowedTesterTypes)
        notice.updated_at = self._now_millis()
        self._db.commit()
        self._db.refresh(notice)
        return self._to_root_access_policy_response(notice)

    def _get_or_create_notice(self) -> AppNoticeModel:
        notice = self._db.query(AppNoticeModel).filter(AppNoticeModel.id == self.DEFAULT_NOTICE_ID).first()
        if notice is not None:
            return notice

        notice = AppNoticeModel(
            id=self.DEFAULT_NOTICE_ID,
            title=settings.client_notice_title,
            message=settings.client_notice_message,
            qq_group_number=settings.client_notice_group_number,
            bilibili_text=settings.client_bilibili_text,
            bilibili_url=settings.client_bilibili_url,
            root_access_allowed_tester_types=serialize_root_access_types(list(DEFAULT_ROOT_ACCESS_TYPES)),
            updated_at=self._now_millis(),
        )
        self._db.add(notice)
        self._db.commit()
        self._db.refresh(notice)
        return notice

    def _to_response(self, notice: AppNoticeModel) -> AppNoticeResponse:
        return AppNoticeResponse(
            title=notice.title,
            message=notice.message,
            qqGroupNumber=notice.qq_group_number,
            bilibiliText=notice.bilibili_text,
            bilibiliUrl=notice.bilibili_url,
            rootAccessAllowedTesterTypes=normalize_root_access_types(
                parse_root_access_types(notice.root_access_allowed_tester_types)
            ),
            updatedAt=notice.updated_at,
        )

    def _to_root_access_policy_response(self, notice: AppNoticeModel) -> RootAccessPolicyResponse:
        return RootAccessPolicyResponse(
            rootAccessAllowedTesterTypes=normalize_root_access_types(
                parse_root_access_types(notice.root_access_allowed_tester_types)
            ),
            updatedAt=notice.updated_at,
        )

    def _now_millis(self) -> int:
        return int(datetime.now(timezone.utc).timestamp() * 1000)
