from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from starlette.middleware.sessions import SessionMiddleware

from app.api.router import api_router
from app.core.config import settings
from app.infrastructure.db.base import Base
from app.infrastructure.db.migrations import migrate_schema
from app.infrastructure.repositories.shared_nfc_sql_repository import SharedNfcSqlRepository
from app.infrastructure.repositories.shared_route_sql_repository import SharedRouteSqlRepository
from app.infrastructure.db.session import engine
from app.infrastructure.db.session import SessionLocal
from app.infrastructure.db.models import app_log as app_log_models  # noqa: F401
from app.infrastructure.db.models import app_notice as app_notice_models  # noqa: F401
from app.infrastructure.db.models import auth as auth_models  # noqa: F401
from app.infrastructure.db.models import nfc as nfc_models  # noqa: F401
from app.infrastructure.db.models import route as route_models  # noqa: F401
from app.infrastructure.db.models import simulation_config as simulation_config_models  # noqa: F401
from app.infrastructure.db.models import tip as tip_models  # noqa: F401
from app.web.routes.admin_panel import router as admin_panel_router


@asynccontextmanager
async def lifespan(_: FastAPI):
    if settings.auto_create_tables:
        Base.metadata.create_all(bind=engine)
        migrate_schema(engine)
    yield


app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_allow_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.add_middleware(
    SessionMiddleware,
    secret_key=settings.admin_session_secret,
    session_cookie="schoolrun_admin_session",
    same_site="lax",
    https_only=False,
)


@app.middleware("http")
async def inject_repositories(request, call_next):
    db = SessionLocal()
    request.state.db = db
    request.state.route_repository = SharedRouteSqlRepository(db)
    request.state.nfc_repository = SharedNfcSqlRepository(db)
    try:
        response = await call_next(request)
    finally:
        db.close()
    return response

app.include_router(api_router, prefix=settings.api_v1_prefix)
app.include_router(admin_panel_router)
