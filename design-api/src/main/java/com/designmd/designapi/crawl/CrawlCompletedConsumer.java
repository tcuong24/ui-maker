package com.designmd.designapi.crawl;

import com.designmd.designapi.messaging.RabbitMqConstants;
import com.designmd.designapi.messaging.event.CrawlCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlCompletedConsumer {

    private final CrawlResultService crawlResultService;

    @RabbitListener(
            queues = RabbitMqConstants.COMPLETED_QUEUE
    )
    public void consume(
            CrawlCompletedEvent event
    ) {
        log.info(
                "Received CrawlCompletedEvent {} "
                        + "for analysis {}",
                event.eventId(),
                event.analysisJobId()
        );

        crawlResultService.handleCompleted(event);
    }
}