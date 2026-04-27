# SchoolRun Backend

FastAPI backend for shared routes and shared NFC.

## Stack

- FastAPI
- SQLAlchemy
- MySQL 5.7+
- Uvicorn
- Python virtual environment (`venv`)

## Local Development

Windows:

```powershell
cd backend
python -m venv .venv
.venv\Scripts\python.exe -m pip install -r requirements.txt
Copy-Item .env.example .env
.venv\Scripts\python.exe -m uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

Linux:

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

## API

- `GET /api/health`
- `GET /api/client-config`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/auth/alerts`
- `GET /api/tips`
- `GET /api/tips/{id}`
- `POST /api/tips`
- `PUT /api/tips/{id}`
- `DELETE /api/tips/{id}`
- `POST /api/tips/import-word`
- `GET /api/shared/simulation-configs`
- `POST /api/shared/simulation-configs`
- `GET /api/shared/simulation-configs/{id}`
- `GET /api/shared/routes`
- `POST /api/shared/routes`
- `GET /api/shared/routes/{id}`
- `GET /api/shared/nfc`
- `POST /api/shared/nfc`

## Admin Panel

Built-in admin panel:

- page: `/admin`
- login API: `POST /api/admin/login`
- route management: list / edit / delete
- NFC management: list / edit / delete
- internal account management: list / create / update
- notice management: get / update

## Server Deployment

The repository includes a `systemd` service template under:

- `backend/deploy/systemd/schoolrun-backend.service`

The default deployment mode is:

- `uvicorn`
- project-local virtual environment
- bind on `0.0.0.0:5000`

This keeps deployment simple and avoids interfering with existing Nginx sites unless you explicitly want reverse proxy later.
