import "dotenv/config";

function required(name: string): string {
  const value = process.env[name];

  if (!value) {
    throw new Error(`Missing environment variable: ${name}`);
  }

  return value;
}

function integer(
  name: string,
  fallback: number,
  minimum: number,
  maximum: number,
): number {
  const rawValue = process.env[name];

  if (!rawValue) {
    return fallback;
  }

  const value = Number(rawValue);

  if (!Number.isInteger(value) || value < minimum || value > maximum) {
    throw new Error(
      `${name} must be an integer between ${minimum} and ${maximum}`,
    );
  }

  return value;
}

export const env = {
  rabbitMqUrl: required("RABBITMQ_URL"),

  exchange:
    process.env.CRAWL_EXCHANGE ??
    "design.crawl.exchange",

  requestedQueue:
    process.env.CRAWL_REQUESTED_QUEUE ??
    "design.crawl.requested.queue",

  requestedRoutingKey:
    process.env.CRAWL_REQUESTED_ROUTING_KEY ??
    "crawl.requested",

  deadLetterExchange:
    process.env.CRAWL_DEAD_LETTER_EXCHANGE ??
    "design.crawl.dlx",

  requestedDeadLetterQueue:
    process.env.CRAWL_REQUESTED_DLQ ??
    "design.crawl.requested.dlq",

  requestedDeadLetterRoutingKey:
    process.env.CRAWL_REQUESTED_DEAD_LETTER_ROUTING_KEY ??
    "crawl.requested.dead",

  completedRoutingKey:
    process.env.CRAWL_COMPLETED_ROUTING_KEY ??
    "crawl.completed",

  failedRoutingKey:
    process.env.CRAWL_FAILED_ROUTING_KEY ??
    "crawl.failed",

  maxCrawlPages: integer("MAX_CRAWL_PAGES", 11, 1, 50),
  navigationTimeoutMs: integer(
    "NAVIGATION_TIMEOUT_MS",
    30_000,
    1_000,
    120_000,
  ),
  crawlJobTimeoutMs: integer(
    "CRAWL_JOB_TIMEOUT_MS",
    120_000,
    5_000,
    600_000,
  ),
  maxDocumentBytes: integer(
    "MAX_DOCUMENT_BYTES",
    5_000_000,
    100_000,
    50_000_000,
  ),
  crawlRetryAttempts: integer("CRAWL_RETRY_ATTEMPTS", 3, 1, 5),
  publishRetryAttempts: integer("PUBLISH_RETRY_ATTEMPTS", 3, 1, 5),
  retryBaseDelayMs: integer("RETRY_BASE_DELAY_MS", 1_000, 100, 30_000),
  publisherConfirmTimeoutMs: integer(
    "PUBLISHER_CONFIRM_TIMEOUT_MS",
    10_000,
    1_000,
    60_000,
  ),
  rabbitConnectionAttempts: integer(
    "RABBIT_CONNECTION_ATTEMPTS",
    10,
    1,
    100,
  ),
};
