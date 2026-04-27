from pydantic import BaseModel


class AppClientConfigResponse(BaseModel):
    noticeTitle: str
    noticeMessage: str
    qqGroupNumber: str
    bilibiliText: str
    bilibiliUrl: str


class AppClientConfigEnvelope(BaseModel):
    data: AppClientConfigResponse
