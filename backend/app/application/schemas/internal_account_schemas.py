from pydantic import BaseModel, Field

from app.application.schemas.auth_schemas import AccountResponse


class CreateInternalAccountRequest(BaseModel):
    username: str = Field(min_length=1, max_length=64)
    password: str = Field(min_length=1, max_length=128)
    remark: str = Field(default="", max_length=255)


class UpdateInternalAccountRequest(BaseModel):
    remark: str = Field(default="", max_length=255)
    status: str = Field(default="active", max_length=32)
    password: str = Field(default="", max_length=128)
    resetBoundDevice: bool = False


class InternalAccountListEnvelope(BaseModel):
    items: list[AccountResponse]


class InternalAccountEnvelope(BaseModel):
    data: AccountResponse
