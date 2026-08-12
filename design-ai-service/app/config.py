from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    rabbitmq_url: str = "amqp://guest:guest@localhost:5672/"

    analysis_exchange: str = "design.analysis.exchange"
    analysis_requested_queue: str = "design.analysis.requested.queue"
    analysis_requested_routing_key: str = "analysis.requested"

    analysis_completed_routing_key: str = "analysis.completed"
    analysis_failed_routing_key: str = "analysis.failed"

    analysis_dead_letter_exchange: str = "design.analysis.dlx"
    analysis_requested_dead_letter_routing_key: str = (
        "analysis.requested.dead"
    )

    model_config = SettingsConfigDict(
        env_file=".env",
        extra="ignore",
    )


settings = Settings()