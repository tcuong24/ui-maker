import amqp, {
  type ChannelModel,
  type ConfirmChannel,
} from "amqplib";

import { env } from "../config/env.js";

export interface RabbitMqConnection {
  connection: ChannelModel;
  channel: ConfirmChannel;
}

export async function connectRabbitMq(): Promise<RabbitMqConnection> {
  let lastError: unknown;

  for (let attempt = 1; attempt <= env.rabbitConnectionAttempts; attempt++) {
    try {
      return await connectOnce();
    } catch (error) {
      lastError = error;

      if (attempt === env.rabbitConnectionAttempts) {
        break;
      }

      const delayMs = Math.min(
        env.retryBaseDelayMs * 2 ** (attempt - 1),
        30_000,
      );

      console.warn(
        `RabbitMQ connection failed (${attempt}/${env.rabbitConnectionAttempts}); retrying in ${delayMs}ms`,
        error,
      );

      await delay(delayMs);
    }
  }

  throw lastError instanceof Error
    ? lastError
    : new Error("Cannot connect to RabbitMQ");
}

async function connectOnce(): Promise<RabbitMqConnection> {
  const connection = await amqp.connect(env.rabbitMqUrl);
  const channel = await connection.createConfirmChannel();

  await channel.assertExchange(
    env.exchange,
    "direct",
    {
      durable: true
    }
  );

  await channel.assertExchange(env.deadLetterExchange, "direct", {
    durable: true,
  });

  await channel.assertQueue(env.requestedDeadLetterQueue, {
    durable: true,
  });

  await channel.bindQueue(
    env.requestedDeadLetterQueue,
    env.deadLetterExchange,
    env.requestedDeadLetterRoutingKey,
  );

  await channel.assertQueue(
    env.requestedQueue,
    {
      durable: true,
      arguments: {
        "x-dead-letter-exchange": env.deadLetterExchange,
        "x-dead-letter-routing-key": env.requestedDeadLetterRoutingKey,
      },
    }
  );

  await channel.bindQueue(
    env.requestedQueue,
    env.exchange,
    env.requestedRoutingKey
  );

  // Mỗi worker chỉ xử lý một job tại một thời điểm.
  await channel.prefetch(1);

  connection.on("error", error => {
    console.error("RabbitMQ connection error", error);
  });

  connection.on("close", () => {
    console.warn("RabbitMQ connection closed");
  });

  return {
    connection,
    channel
  };
}

function delay(milliseconds: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}
