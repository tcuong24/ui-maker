package com.designmd.designapi.ai;

import com.designmd.designapi.ai.event.DesignAnalysisRequestedEvent;
import com.designmd.designapi.design.DesignSystemSnapshot;
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
public class DesignAnalysisPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(DesignSystemSnapshot snapshot) {
        DesignAnalysisRequestedEvent event =
                new DesignAnalysisRequestedEvent(
                        UUID.randomUUID().toString(),
                        1,
                        snapshot.getAnalysisJobId(),
                        snapshot.getPageCount(),
                        snapshot.getColors(),
                        snapshot.getTypography(),
                        snapshot.getSpacing(),
                        snapshot.getRadii(),
                        snapshot.getShadows(),
                        snapshot.getCssVariables(),
                        Instant.now()
                );

        CorrelationData correlationData =
                new CorrelationData(event.eventId());

        rabbitTemplate.convertAndSend(
                RabbitMqConstants.ANALYSIS_EXCHANGE,
                RabbitMqConstants.ANALYSIS_REQUESTED_KEY,
                event,
                correlationData
        );

        waitForConfirmation(correlationData);
    }

    private void waitForConfirmation(
            CorrelationData correlationData
    ) {
        try {
            CorrelationData.Confirm confirm =
                    correlationData.getFuture()
                            .get(10, TimeUnit.SECONDS);

            if (!confirm.isAck()) {
                throw new AmqpException(
                        "RabbitMQ rejected analysis event: "
                                + confirm.getReason()
                );
            }

            if (correlationData.getReturned() != null) {
                throw new AmqpException(
                        "Analysis event was unroutable"
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new AmqpException(
                    "Interrupted while publishing analysis event",
                    exception
            );
        } catch (
                ExecutionException |
                TimeoutException exception
        ) {
            throw new AmqpException(
                    "Cannot confirm analysis event publication",
                    exception
            );
        }
    }
}