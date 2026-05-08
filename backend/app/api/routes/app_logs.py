from fastapi import APIRouter, Depends

from app.api.dependencies import get_db, get_optional_account
from app.application.schemas.app_log_schemas import (
    InternalSoftwareNameEnvelope,
    InternalSoftwareNameListEnvelope,
    SubmitInternalSoftwareNameRequest,
    UploadAppLogEnvelope,
    UploadAppLogRequest,
)
from app.application.schemas.auth_schemas import AccountResponse
from app.application.services.app_log_service import AppLogService

router = APIRouter(prefix="/app-logs")


@router.get("/software-names", response_model=InternalSoftwareNameListEnvelope)
def list_software_names(db=Depends(get_db)) -> InternalSoftwareNameListEnvelope:
    return InternalSoftwareNameListEnvelope(items=AppLogService(db).list_approved_software_names())


@router.post("/software-name-submissions", response_model=InternalSoftwareNameEnvelope)
def submit_software_name(
        payload: SubmitInternalSoftwareNameRequest,
        db=Depends(get_db),
        account: AccountResponse | None = Depends(get_optional_account),
) -> InternalSoftwareNameEnvelope:
    return InternalSoftwareNameEnvelope(data=AppLogService(db).submit_software_name(payload, account))


@router.post("", response_model=UploadAppLogEnvelope)
def upload_app_log(
        payload: UploadAppLogRequest,
        db=Depends(get_db),
        account: AccountResponse | None = Depends(get_optional_account),
) -> UploadAppLogEnvelope:
    return UploadAppLogEnvelope(data=AppLogService(db).upload_log(payload, account))
