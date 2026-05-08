from fastapi import APIRouter, Depends, HTTPException, Query, status

from app.api.dependencies import get_db, get_optional_account
from app.application.schemas.auth_schemas import AccountResponse
from app.application.schemas.simulation_config_schemas import (
    SaveSharedSimulationConfigRequest,
    SharedSimulationConfigEnvelope,
    SharedSimulationConfigListEnvelope,
)
from app.application.services.shared_simulation_config_service import SharedSimulationConfigService
from app.application.services.app_notice_service import AppNoticeService
from app.core.exceptions import ResourceNotFoundError

router = APIRouter(prefix="/shared/simulation-configs")


@router.get("", response_model=SharedSimulationConfigListEnvelope)
def list_configs(
    q: str = Query(default=""),
    db=Depends(get_db),
) -> SharedSimulationConfigListEnvelope:
    return SharedSimulationConfigListEnvelope(items=SharedSimulationConfigService(db).list_configs(q))


@router.post("", response_model=SharedSimulationConfigEnvelope, status_code=status.HTTP_201_CREATED)
def create_config(
    payload: SaveSharedSimulationConfigRequest,
    account: AccountResponse | None = Depends(get_optional_account),
    db=Depends(get_db),
) -> SharedSimulationConfigEnvelope:
    root_access_allowed_tester_types = AppNoticeService(db).get_notice().rootAccessAllowedTesterTypes
    return SharedSimulationConfigEnvelope(
        data=SharedSimulationConfigService(db).create_config(payload, account, root_access_allowed_tester_types)
    )


@router.get("/{config_id}", response_model=SharedSimulationConfigEnvelope)
def get_config(
    config_id: str,
    db=Depends(get_db),
) -> SharedSimulationConfigEnvelope:
    service = SharedSimulationConfigService(db)
    try:
        config = service.get_config(config_id)
    except ResourceNotFoundError as exception:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exception)) from exception
    return SharedSimulationConfigEnvelope(data=config)
