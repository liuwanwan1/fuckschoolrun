# Deployment Notes

## Recommended Mode

- Python virtual environment: `backend/.venv`
- Process manager: `systemd`
- App server: `uvicorn`
- Public port: `5000`

## Why This Mode

- the target server already exposes `5000/tcp`
- no Docker is required
- no mandatory Nginx change is required
- avoids interfering with existing sites on ports `80` and `888`

## Expected Public Base URL

```text
http://<server-ip>:5000/
```

Android `SHARE_API_BASE_URL` example:

```properties
SHARE_API_BASE_URL=http://47.113.226.102:5000/
```

## Runtime Files

- app code: `/www/wwwroot/fuckschoolrun/backend`
- environment file: `/www/wwwroot/fuckschoolrun/backend/.env`
- virtual environment: `/www/wwwroot/fuckschoolrun/backend/.venv`
- service file: `/etc/systemd/system/schoolrun-backend.service`

## Optional Reverse Proxy

If you want to proxy through Nginx later, a location example is already included in:

- `backend/deploy/nginx/schoolrun-backend.conf`
