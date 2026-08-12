package com.designmd.designapi.ai;

import com.designmd.designapi.ai.event.DesignAnalysisCompletedEvent;
import com.designmd.designapi.messaging.RabbitMqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DesignAnalysisCompletedConsumer {

    private final DesignAnalysisResultService resultService;

    @RabbitListener(
            queues =
                    RabbitMqConstants.ANALYSIS_COMPLETED_QUEUE
    )
    public void consume(
            DesignAnalysisCompletedEvent event
    ) {
        log.info(
                "Received DesignAnalysisCompletedEvent {} "
                        + "for analysis {}",
                event.eventId(),
                event.analysisJobId()
        );

        resultService.handleCompleted(event);

        log.info(
                "Saved style and Markdown for analysis {}",
                event.analysisJobId()
        );
    }
}