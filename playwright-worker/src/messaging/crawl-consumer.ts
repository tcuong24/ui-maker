import type { ConfirmChannel, ConsumeMessage } from "amqplib";

import { env } from "../config/env.js";
import { CrawlService } from "../crawler/crawl-service.js";

import { ResultPublisher } from "./result-publisher.js";
import { CrawlRequestedEvent } from "../contracts/crawl-request-event.js";

export class CrawlConsumer {
  constructor(
    private readonly channel: ConfirmChannel,
    private readonly crawlService: CrawlService,
    private readonly publisher: ResultPublisher,
  ) {}

  async start(): Promise<void> {
    await this.channel.consume(
      env.requestedQueue,
      (message) => {
        if (!message) {
          return;
        }

        void this.handle(message);
      },
      {
        noAck: false,
      },
    );

    console.log(`Waiting for messages: ${env.requestedQueue}`);
  }

  private async handle(message: ConsumeMessage): Promise<void> {
    let event: CrawlRequestedEvent | undefined;
    let validEvent = false;
    let crawlCompleted = false;

    try {
      event = JSON.parse(
        message.content.toString("utf-8"),
      ) as CrawlRequestedEvent;

      this.validate(event);
      validEvent = true;

      console.log(
        `Starting analysis ${event.analysisJobId}:`,
        event.websiteUrl,
      );

      const startedAt = new Date();
      const pages = await this.crawlWithRetry(event);
      crawlCompleted = true;

      await this.publisher.publishCompleted(event, pages, startedAt);

      this.channel.ack(message);

      console.log(`Completed analysis ${event.analysisJobId}`);
    } catch (error) {
      console.error("Crawl failed", error);

      if (event && validEvent && crawlCompleted) {
        console.error(
          "Cannot publish CrawlCompletedEvent; dead-lettering request",
          error,
        );
        this.channel.nack(message, false, false);
        return;
      }

      if (event && validEvent) {
        try {
          await this.publisher.publishFailed(event, error);

        // Lỗi crawl đã được gửi về Design API.
          this.channel.ack(message);
        } catch (publishError) {
          console.error(
            "Cannot publish CrawlFailedEvent; dead-lettering request",
            publishError,
          );
          this.channel.nack(message, false, false);
        }
        return;
      }

      // JSON/event không hợp lệ, không retry vô hạn.
      this.channel.nack(message, false, false);
    }
  }

  private async crawlWithRetry(
    event: CrawlRequestedEvent,
  ): Promise<Awaited<ReturnType<CrawlService["crawl"]>>> {
    let lastError: unknown;

    for (let attempt = 1; attempt <= env.crawlRetryAttempts; attempt++) {
      try {
        return await this.crawlService.crawl(event);
      } catch (error) {
        lastError = error;

        if (attempt === env.crawlRetryAttempts) {
          break;
        }

        const delayMs = Math.min(
          env.retryBaseDelayMs * 2 ** (attempt - 1),
          30_000,
        );

        console.warn(
          `Crawl attempt failed (${attempt}/${env.crawlRetryAttempts}); retrying in ${delayMs}ms`,
          error,
        );

        await new Promise((resolve) => setTimeout(resolve, delayMs));
      }
    }

    throw lastError instanceof Error
      ? lastError
      : new Error("Crawl failed after retries");
  }

  private validate(event: CrawlRequestedEvent): void {
    if (!event.eventId || !event.analysisJobId || !event.websiteUrl) {
      throw new Error("Invalid CrawlRequestedEvent");
    }

    const url = new URL(event.websiteUrl);

    if (!["http:", "https:"].includes(url.protocol)) {
      throw new Error("Only HTTP and HTTPS URLs are supported");
    }
  }
}
