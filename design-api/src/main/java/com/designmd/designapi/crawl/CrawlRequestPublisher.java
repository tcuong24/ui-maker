package com.designmd.designapi.crawl;

import com.designmd.designapi.analysis.AnalysisJob;
import com.designmd.designapi.crawl.request.CrawlRequestedEvent;
import com.designmd.designapi.messaging.RabbitMqConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class CrawlRequestPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publish(AnalysisJob job) {
        CrawlRequestedEvent event = new CrawlRequestedEvent(
                UUID.randomUUID().toString(),
                1,
                job.getId(),
                job.getWebsiteUrl(),
                job.getAdditionalPaths(),
                job.isIncludeScreenshot(),
                Instant.now()
        );

        CorrelationData correlationData =
                new CorrelationData(event.eventId());

        rabbitTemplate.convertAndSend(
                RabbitMqConstants.EXCHANGE,
                RabbitMqConstants.REQUESTED_KEY,
                event,
                correlationData
        );

        waitForConfirmation(correlationData);
    }

    private void waitForConfirmation(
            CorrelationData correlationData
    ) {
        try {
            CorrelationData.Confirm confirm = correlationData
                    .getFuture()
                    .get(10, TimeUnit.SECONDS);

            if (!confirm.isAck()) {
                throw new AmqpException(
                        "RabbitMQ rejected crawl event: "
                                + confirm.getReason()
                );
            }

            if (correlationData.getReturned() != null) {
                throw new AmqpException(
                        "Crawl event was returned as unroutable"
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AmqpException(
                    "Interrupted while waiting for RabbitMQ confirm",
                    exception
            );
        } catch (ExecutionException | TimeoutException exception) {
            throw new AmqpException(
                    "Cannot confirm crawl event publication",
                    exception
            );
        }
    }
}
