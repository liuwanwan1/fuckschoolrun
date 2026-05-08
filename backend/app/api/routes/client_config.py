from fastapi import APIRouter, Depends

from app.api.dependencies import get_db
from app.application.schemas.client_config_schemas import (
    AppClientConfigEnvelope,
    AppClientConfigResponse,
)
from app.application.services.app_notice_service import AppNoticeService

router = APIRouter()


@router.get("/client-config", response_model=AppClientConfigEnvelope)
def get_client_config(db=Depends(get_db)) -> AppClientConfigEnvelope:
    notice = AppNoticeService(db).get_notice()
    return AppClientConfigEnvelope(
        data=AppClientConfigResponse(
            noticeTitle=notice.title,
            noticeMessage=notice.message,
            qqGroupNumber=notice.qqGroupNumber,
            bilibiliText=notice.bilibiliText,
            bilibiliUrl=notice.bilibiliUrl,
            rootAccessAllowedTesterTypes=notice.rootAccessAllowedTesterTypes,
        )
    )
