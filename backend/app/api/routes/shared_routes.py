from fastapi import APIRouter, Depends, HTTPException, status

from app.api.dependencies import get_route_service
from app.application.schemas.route_schemas import (
    CreateSharedRouteRequest,
    SharedRouteDetailEnvelope,
    SharedRouteListEnvelope,
)
from app.application.services.shared_route_service import SharedRouteService
from app.core.exceptions import ResourceNotFoundError

router = APIRouter()


@router.get("", response_model=SharedRouteListEnvelope)
def list_shared_routes(
    route_service: SharedRouteService = Depends(get_route_service),
) -> SharedRouteListEnvelope:
    return SharedRouteListEnvelope(items=route_service.list_routes())


@router.post("", response_model=SharedRouteDetailEnvelope, status_code=status.HTTP_201_CREATED)
def create_shared_route(
    payload: CreateSharedRouteRequest,
    route_service: SharedRouteService = Depends(get_route_service),
) -> SharedRouteDetailEnvelope:
    return SharedRouteDetailEnvelope(data=route_service.create_route(payload))


@router.get("/{route_id}", response_model=SharedRouteDetailEnvelope)
def get_shared_route(
    route_id: str,
    route_service: SharedRouteService = Depends(get_route_service),
) -> SharedRouteDetailEnvelope:
    try:
        route = route_service.get_route(route_id)
    except ResourceNotFoundError as exception:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exception)) from exception
    return SharedRouteDetailEnvelope(data=route)
