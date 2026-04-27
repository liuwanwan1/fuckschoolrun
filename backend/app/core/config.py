from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    app_name: str = "SchoolRun Shared Backend"
    app_env: str = "development"
    app_version: str = "1.0.0"
    api_v1_prefix: str = "/api"
    server_host: str = "0.0.0.0"
    server_port: int = 8000
    auto_create_tables: bool = True
    sql_echo: bool = False

    cors_allow_origins_raw: str = Field(default="*", alias="CORS_ALLOW_ORIGINS")

    mysql_host: str = "127.0.0.1"
    mysql_port: int = 3306
    mysql_database: str = "schoolrun_shared"
    mysql_user: str = "schoolrun_app"
    mysql_password: str = "change_me"
    mysql_charset: str = "utf8mb4"

    admin_username: str = "admin"
    admin_password: str = "change_me_admin"
    admin_session_secret: str = "change_me_session_secret"
    admin_baidu_maps_ak: str = ""
    mobile_auth_secret: str = "change_me_mobile_auth_secret"
    mobile_auth_max_age_seconds: int = 60 * 60 * 24 * 30
    internal_auth_variant: str = "schoolrun_toolbox"
    auth_wrong_password_window_seconds: int = 10 * 60
    auth_wrong_password_max_attempts: int = 5
    auth_wrong_password_first_ban_seconds: int = 24 * 60 * 60
    auth_wrong_device_max_attempts: int = 3
    client_notice_title: str = "使用说明"
    client_notice_message: str = (
        "本软件为完全免费开源软件，基于影梭打造，仅供学习交流，禁止盈利，"
        "目前仅测试过阳光校园跑，其他的你们自行测试，QQ群号：951300122"
    )
    client_notice_group_number: str = "951300122"
    client_bilibili_text: str = "此为 bilibili UP 主开发的免费开源软件"
    client_bilibili_url: str = "https://space.bilibili.com/1492911803?spm_id_from=333.1007.0.0"

    @property
    def cors_allow_origins(self) -> list[str]:
        if self.cors_allow_origins_raw.strip() == "*":
            return ["*"]
        return [item.strip() for item in self.cors_allow_origins_raw.split(",") if item.strip()]

    @property
    def database_url(self) -> str:
        return (
            f"mysql+pymysql://{self.mysql_user}:{self.mysql_password}"
            f"@{self.mysql_host}:{self.mysql_port}/{self.mysql_database}"
            f"?charset={self.mysql_charset}"
        )


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
