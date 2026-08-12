package com.designmd.designapi.crawl;

import com.designmd.designapi.messaging.RabbitMqConstants;
import com.designmd.designapi.messaging.event.CrawlFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlFailedConsumer {

    private final CrawlResultService crawlResultService;

    @RabbitListener(
            queues = RabbitMqConstants.FAILED_QUEUE
    )
    public void consume(
            CrawlFailedEvent event
    ) {
        log.warn(
                "Received CrawlFailedEvent {} "
                        + "for analysis {}",
                event.eventId(),
                event.analysisJobId()
        );

        crawlResultService.handleFailed(event);
    }
}