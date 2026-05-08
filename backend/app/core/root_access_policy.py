from app.application.schemas.auth_schemas import AccountResponse
from app.core.tester_account import VALID_TESTER_TYPES

ANONYMOUS_ROOT_ACCESS_TYPE = "anonymous"
DEFAULT_ROOT_ACCESS_TYPES = ("ordinary", "advanced", "pioneer")
VALID_ROOT_ACCESS_TYPES = set(VALID_TESTER_TYPES) | {ANONYMOUS_ROOT_ACCESS_TYPE}


def normalize_root_access_types(values: list[str] | tuple[str, ...] | None, *, use_default_when_missing: bool = True) -> list[str]:
    if values is None:
        source = DEFAULT_ROOT_ACCESS_TYPES if use_default_when_missing else ()
    else:
        source = values

    normalized: list[str] = []
    for value in source:
        root_type = normalize_root_access_type(value)
        if root_type and root_type not in normalized:
            normalized.append(root_type)
    return normalized


def parse_root_access_types(value: str | None, *, use_default_when_missing: bool = True) -> list[str]:
    if value is None:
        return normalize_root_access_types(None, use_default_when_missing=use_default_when_missing)
    parts = [part.strip() for part in value.split(",")]
    return normalize_root_access_types(parts, use_default_when_missing=False)


def serialize_root_access_types(values: list[str] | tuple[str, ...] | None) -> str:
    return ",".join(normalize_root_access_types(values, use_default_when_missing=False))


def can_identity_use_root(account: AccountResponse | None, allowed_types: list[str] | tuple[str, ...]) -> bool:
    normalized_allowed = normalize_root_access_types(allowed_types, use_default_when_missing=False)
    if account is None:
        return ANONYMOUS_ROOT_ACCESS_TYPE in normalized_allowed
    return account.status == "active" and account.testerType in normalized_allowed


def normalize_root_access_type(value: str | None) -> str:
    normalized = (value or "").strip().lower()
    if normalized in {"guest", "unauthenticated", "not_logged_in", "not-logged-in"}:
        normalized = ANONYMOUS_ROOT_ACCESS_TYPE
    if normalized not in VALID_ROOT_ACCESS_TYPES:
        return ""
    return normalized
