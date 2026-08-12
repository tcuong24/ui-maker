from datetime import datetime
from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class DesignAnalysisRequestedEvent(BaseModel):
    event_id: str = Field(alias="eventId")
    schema_version: int = Field(alias="schemaVersion")
    analysis_job_id: str = Field(alias="analysisJobId")
    page_count: int = Field(alias="pageCount")

    colors: list[dict[str, Any]] = Field(
        default_factory=list,
    )

    typography: list[dict[str, Any]] = Field(
        default_factory=list,
    )

    spacing: list[dict[str, Any]] = Field(
        default_factory=list,
    )

    radii: list[dict[str, Any]] = Field(
        default_factory=list,
    )

    shadows: list[dict[str, Any]] = Field(
        default_factory=list,
    )

    css_variables: list[dict[str, Any]] = Field(
        default_factory=list,
        alias="cssVariables",
    )

    occurred_at: datetime = Field(alias="occurredAt")

    model_config = ConfigDict(
        populate_by_name=True,
        extra="forbid",
    )


class DesignAnalysisCompletedEvent(BaseModel):
    event_id: str = Field(alias="eventId")
    schema_version: int = Field(alias="schemaVersion")
    source_event_id: str = Field(alias="sourceEventId")
    analysis_job_id: str = Field(alias="analysisJobId")

    style: dict[str, Any]

    markdown_content: str = Field(
        alias="markdownContent",
    )

    confidence: float

    completed_at: datetime = Field(
        alias="completedAt",
    )

    model_config = ConfigDict(
        populate_by_name=True,
        extra="forbid",
    )