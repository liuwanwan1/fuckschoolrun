from pydantic import BaseModel, Field


class AppNoticeResponse(BaseModel):
    title: str
    message: str
    qqGroupNumber: str
    bilibiliText: str
    bilibiliUrl: str
    rootAccessAllowedTesterTypes: list[str] = Field(default_factory=list)
    updatedAt: int


class UpdateAppNoticeRequest(BaseModel):
    title: str = Field(min_length=1, max_length=128)
    message: str = Field(min_length=1, max_length=20_000)
    qqGroupNumber: str = Field(default="", max_length=64)
    bilibiliText: str = Field(default="", max_length=255)
    bilibiliUrl: str = Field(default="", max_length=512)
    rootAccessAllowedTesterTypes: list[str] | None = None


class AppNoticeEnvelope(BaseModel):
    data: AppNoticeResponse


class RootAccessPolicyResponse(BaseModel):
    rootAccessAllowedTesterTypes: list[str] = Field(default_factory=list)
    updatedAt: int


class UpdateRootAccessPolicyRequest(BaseModel):
    rootAccessAllowedTesterTypes: list[str] = Field(default_factory=list)


class RootAccessPolicyEnvelope(BaseModel):
    data: RootAccessPolicyResponse
