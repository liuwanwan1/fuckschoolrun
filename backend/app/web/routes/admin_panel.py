from pathlib import Path

from fastapi import APIRouter, Request, Response
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates

from app.web.auth import is_admin_authenticated

router = APIRouter()
templates = Jinja2Templates(directory=str(Path(__file__).resolve().parents[1] / "templates"))


@router.get("/admin", response_class=HTMLResponse)
def admin_page(request: Request):
    return templates.TemplateResponse(
        request=request,
        name="admin.html",
        context={
            "authenticated": is_admin_authenticated(request),
            "admin_username": request.session.get("admin_username"),
        },
    )


@router.get("/favicon.ico", include_in_schema=False)
def favicon():
    return Response(status_code=204)
