import { connectRabbitMq } from "./messaging/rabbitmq.js";
import { CrawlService } from "./crawler/crawl-service.js";
import { CrawlConsumer } from "./messaging/crawl-consumer.js";
import { ResultPublisher } from "./messaging/result-publisher.js";

async function main(): Promise<void> {
  const { connection, channel } =
    await connectRabbitMq();

  const crawlService = new CrawlService();
  await crawlService.start();

  const publisher =
    new ResultPublisher(channel);

  const consumer = new CrawlConsumer(
    channel,
    crawlService,
    publisher
  );

  await consumer.start();

  const shutdown = async (
    signal: string
  ): Promise<void> => {
    console.log(`Received ${signal}, shutting down`);

    await crawlService.stop();
    await channel.close();
    await connection.close();

    process.exit(0);
  };

  process.on(
    "SIGINT",
    () => void shutdown("SIGINT")
  );

  process.on(
    "SIGTERM",
    () => void shutdown("SIGTERM")
  );

  console.log("Playwright worker started");
}

main().catch(error => {
  console.error(
    "Cannot start Playwright worker",
    error
  );

  process.exit(1);
});