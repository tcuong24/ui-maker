import asyncio
import logging

from app.messaging.rabbitmq import RabbitMqWorker


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s - %(message)s",
)


async def main() -> None:
    worker = RabbitMqWorker()
    await worker.start()


if __name__ == "__main__":
    asyncio.run(main())