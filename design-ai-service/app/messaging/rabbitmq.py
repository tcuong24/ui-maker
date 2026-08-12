import asyncio
import json
import logging
from datetime import datetime, timezone
from uuid import uuid4

import aio_pika

from app.analyzers.design_analyzer import DesignAnalyzer
from app.config import settings
from app.contracts.events import (
    DesignAnalysisCompletedEvent,
    DesignAnalysisRequestedEvent,
)
from app.generators.markdown_generator import MarkdownGenerator


logger = logging.getLogger(__name__)


class RabbitMqWorker:
    def __init__(self) -> None:
        self.analyzer = DesignAnalyzer()
        self.markdown_generator = MarkdownGenerator()
        self.exchange = None

    async def start(self) -> None:
        connection = await aio_pika.connect_robust(
            settings.rabbitmq_url,
        )

        channel = await connection.channel(
            publisher_confirms=True,
            on_return_raises=True,
        )

        await channel.set_qos(prefetch_count=1)

        self.exchange = await channel.declare_exchange(
            settings.analysis_exchange,
            aio_pika.ExchangeType.DIRECT,
            durable=True,
        )

        await channel.declare_exchange(
            settings.analysis_dead_letter_exchange,
            aio_pika.ExchangeType.DIRECT,
            durable=True,
        )

        queue = await channel.declare_queue(
            settings.analysis_requested_queue,
            durable=True,
            arguments={
                "x-dead-letter-exchange":
                    settings.analysis_dead_letter_exchange,
                "x-dead-letter-routing-key":
                    settings.analysis_requested_dead_letter_routing_key,
            },
        )

        await queue.bind(
            self.exchange,
            settings.analysis_requested_routing_key,
        )

        await queue.consume(self.handle_message)

        logger.info(
            "Waiting for messages: %s",
            settings.analysis_requested_queue,
        )

        await asyncio.Future()

    async def handle_message(
            self,
            message: aio_pika.IncomingMessage,
    ) -> None:
        async with message.process(requeue=False):
            payload = json.loads(message.body)

            request = (
                DesignAnalysisRequestedEvent.model_validate(
                    payload,
                )
            )

            analysis = self.analyzer.analyze(request)

            markdown = self.markdown_generator.generate(
                analysis_job_id=request.analysis_job_id,
                style=analysis["style"],
                recommendations=analysis["recommendations"],
                confidence=analysis["confidence"],
            )

            completed = DesignAnalysisCompletedEvent(
                eventId=str(uuid4()),
                schemaVersion=1,
                sourceEventId=request.event_id,
                analysisJobId=request.analysis_job_id,
                style=analysis["style"],
                markdownContent=markdown,
                confidence=analysis["confidence"],
                completedAt=datetime.now(timezone.utc),
            )

            body = completed.model_dump_json(
                by_alias=True,
            ).encode("utf-8")

            await self.exchange.publish(
                aio_pika.Message(
                    body=body,
                    content_type="application/json",
                    delivery_mode=(
                        aio_pika.DeliveryMode.PERSISTENT
                    ),
                    message_id=completed.event_id,
                    correlation_id=request.event_id,
                ),
                routing_key=(
                    settings.analysis_completed_routing_key
                ),
                mandatory=True,
            )

            logger.info(
                "Published style and Markdown for %s",
                request.analysis_job_id,
            )