from fastapi import APIRouter, Request, status
from fastapi.responses import JSONResponse

from app.application.schemas.admin_schemas import (
    ActionMessageResponse,
    AdminLoginRequest,
    AdminSessionResponse,
)
from app.application.schemas.nfc_schemas import (
    CreateSharedNfcRequest,
    SharedNfcEnvelope,
    SharedNfcListEnvelope,
)
from app.application.schemas.route_schemas import (
    CreateSharedRouteRequest,
    SharedRouteAdminListEnvelope,
    SharedRouteDetailEnvelope,
)
from app.application.services.admin_auth_service import AdminAuthService
from app.application.services.admin_nfc_service import AdminNfcService
from app.application.services.admin_route_service import AdminRouteService
from app.core.exceptions import ResourceNotFoundError
from app.web.auth import ADMIN_SESSION_KEY, require_admin_session

router = APIRouter(prefix="/admin")


@router.post("/login", response_model=AdminSessionResponse)
def admin_login(request: Request, payload: AdminLoginRequest):
    auth_service = AdminAuthService()
    if not auth_service.verify_credentials(payload.username, payload.password):
        return JSONResponse(
            status_code=status.HTTP_401_UNAUTHORIZED,
            content={"detail": "账号或密码错误"},
        )

    request.session[ADMIN_SESSION_KEY] = payload.username
    return AdminSessionResponse(authenticated=True, username=payload.username)


@router.post("/logout", response_model=ActionMessageResponse)
def admin_logout(request: Request):
    request.session.clear()
    return ActionMessageResponse(message="已退出登录")


@router.get("/me", response_model=AdminSessionResponse)
def admin_me(request: Request):
    username = request.session.get(ADMIN_SESSION_KEY)
    return AdminSessionResponse(authenticated=bool(username), username=username)


@router.get("/routes", response_model=SharedRouteAdminListEnvelope)
def admin_list_routes(request: Request):
    require_admin_session(request)
    route_service = AdminRouteService(request.state.route_repository)
    return SharedRouteAdminListEnvelope(items=route_service.list_routes())


@router.put("/routes/{route_id}", response_model=SharedRouteDetailEnvelope)
def admin_update_route(route_id: str, payload: CreateSharedRouteRequest, request: Request):
    require_admin_session(request)
    route_service = AdminRouteService(request.state.route_repository)
    try:
        route = route_service.update_route(route_id, payload)
    except ResourceNotFoundError as exception:
        return JSONResponse(
            status_code=status.HTTP_404_NOT_FOUND,
            content={"detail": str(exception)},
        )
    return SharedRouteDetailEnvelope(data=route)


@router.delete("/routes/{route_id}", response_model=ActionMessageResponse)
def admin_delete_route(route_id: str, request: Request):
    require_admin_session(request)
    route_service = AdminRouteService(request.state.route_repository)
    try:
        route_service.delete_route(route_id)
    except ResourceNotFoundError as exception:
        return JSONResponse(
            status_code=status.HTTP_404_NOT_FOUND,
            content={"detail": str(exception)},
        )
    return ActionMessageResponse(message="共享路线已删除")


@router.get("/nfc", response_model=SharedNfcListEnvelope)
def admin_list_nfc(request: Request):
    require_admin_session(request)
    nfc_service = AdminNfcService(request.state.nfc_repository)
    return SharedNfcListEnvelope(items=nfc_service.list_entries())


@router.put("/nfc/{entry_id}", response_model=SharedNfcEnvelope)
def admin_update_nfc(entry_id: str, payload: CreateSharedNfcRequest, request: Request):
    require_admin_session(request)
    nfc_service = AdminNfcService(request.state.nfc_repository)
    try:
        entry = nfc_service.update_entry(entry_id, payload)
    except ResourceNotFoundError as exception:
        return JSONResponse(
            status_code=status.HTTP_404_NOT_FOUND,
            content={"detail": str(exception)},
        )
    return SharedNfcEnvelope(data=entry)


@router.delete("/nfc/{entry_id}", response_model=ActionMessageResponse)
def admin_delete_nfc(entry_id: str, request: Request):
    require_admin_session(request)
    nfc_service = AdminNfcService(request.state.nfc_repository)
    try:
        nfc_service.delete_entry(entry_id)
    except ResourceNotFoundError as exception:
        return JSONResponse(
            status_code=status.HTTP_404_NOT_FOUND,
            content={"detail": str(exception)},
        )
    return ActionMessageResponse(message="共享 NFC 已删除")
