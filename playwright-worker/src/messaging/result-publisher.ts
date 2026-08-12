import { randomUUID } from "node:crypto";
import type { ConfirmChannel, Message } from "amqplib";

import { env } from "../config/env.js";
import {
  CrawlCompletedEvent,
  CrawledPageResult,
  CrawlFailedEvent,
  CrawlRequestedEvent,
} from "../contracts/crawl-request-event.js";

export class ResultPublisher {
  constructor(private readonly channel: ConfirmChannel) {}

  async publishCompleted(
    request: CrawlRequestedEvent,
    pages: CrawledPageResult[],
    startedAt: Date,
  ): Promise<void> {
    const event: CrawlCompletedEvent = {
      eventId: randomUUID(),
      schemaVersion: 1,
      sourceEventId: request.eventId,
      analysisJobId: request.analysisJobId,
      pages,
      startedAt: startedAt.toISOString(),
      completedAt: new Date().toISOString(),
    };

    await this.publish(env.completedRoutingKey, event);
  }

  async publishFailed(
    request: CrawlRequestedEvent,
    error: unknown,
  ): Promise<void> {
    const event: CrawlFailedEvent = {
      eventId: randomUUID(),
      schemaVersion: 1,
      sourceEventId: request.eventId,
      analysisJobId: request.analysisJobId,
      errorCode: "CRAWL_FAILED",
      errorMessage:
        error instanceof Error ? error.message : "Unknown crawl error",
      failedAt: new Date().toISOString(),
    };

    await this.publish(env.failedRoutingKey, event);
  }

  private async publish(routingKey: string, event: object): Promise<void> {
    let lastError: unknown;

    for (let attempt = 1; attempt <= env.publishRetryAttempts; attempt++) {
      try {
        await this.publishOnce(routingKey, event);
        return;
      } catch (error) {
        lastError = error;

        if (attempt === env.publishRetryAttempts) {
          break;
        }

        const delayMs = Math.min(
          env.retryBaseDelayMs * 2 ** (attempt - 1),
          30_000,
        );

        console.warn(
          `Publish failed for ${routingKey} (${attempt}/${env.publishRetryAttempts}); retrying in ${delayMs}ms`,
          error,
        );

        await delay(delayMs);
      }
    }

    throw lastError instanceof Error
      ? lastError
      : new Error(`Cannot publish RabbitMQ message: ${routingKey}`);
  }

  private publishOnce(routingKey: string, event: object): Promise<void> {
    return new Promise((resolve, reject) => {
      const eventId = (event as { eventId?: unknown }).eventId;
      const messageId =
        typeof eventId === "string" ? eventId : randomUUID();
      let confirmed = false;
      let drained = false;
      let settled = false;

      let timeout: NodeJS.Timeout;

      const cleanup = (): void => {
        clearTimeout(timeout);
        this.channel.off("return", onReturn);
      };

      const finish = (): void => {
        if (!settled && confirmed && drained) {
          settled = true;
          cleanup();
          resolve();
        }
      };

      const fail = (error: Error): void => {
        if (!settled) {
          settled = true;
          cleanup();
          reject(error);
        }
      };

      const onReturn = (message: Message): void => {
        if (message.properties.messageId === messageId) {
          fail(new Error(`RabbitMQ returned unroutable message: ${routingKey}`));
        }
      };

      this.channel.on("return", onReturn);

      timeout = setTimeout(() => {
        fail(new Error(`Publisher confirm timed out: ${routingKey}`));
      }, env.publisherConfirmTimeoutMs);

      try {
        const writable = this.channel.publish(
          env.exchange,
          routingKey,
          Buffer.from(JSON.stringify(event)),
          {
            contentType: "application/json",
            contentEncoding: "utf-8",
            deliveryMode: 2,
            mandatory: true,
            messageId,
          },
          (error) => {
            if (error) {
              fail(error);
              return;
            }

            confirmed = true;
            finish();
          },
        );

        drained = writable;

        if (!writable) {
          this.channel.once("drain", () => {
            drained = true;
            finish();
          });
        }

        finish();
      } catch (error) {
        fail(error instanceof Error ? error : new Error(String(error)));
      }
    });
  }
}

function delay(milliseconds: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}
