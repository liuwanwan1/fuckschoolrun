from pydantic import BaseModel, Field


class AppClientConfigResponse(BaseModel):
    noticeTitle: str
    noticeMessage: str
    qqGroupNumber: str
    bilibiliText: str
    bilibiliUrl: str
    rootAccessAllowedTesterTypes: list[str] = Field(default_factory=list)


class AppClientConfigEnvelope(BaseModel):
    data: AppClientConfigResponse
