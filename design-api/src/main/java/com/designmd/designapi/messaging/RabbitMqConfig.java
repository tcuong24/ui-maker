package com.designmd.designapi.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class RabbitMqConfig {

    @Bean
    DirectExchange crawlExchange() {
        return new DirectExchange(
                RabbitMqConstants.EXCHANGE,
                true,
                false
        );
    }

    @Bean
    DirectExchange crawlDeadLetterExchange() {
        return new DirectExchange(
                RabbitMqConstants.DEAD_LETTER_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    Queue crawlRequestedQueue() {
        return QueueBuilder
                .durable(RabbitMqConstants.REQUESTED_QUEUE)
                .deadLetterExchange(
                        RabbitMqConstants.DEAD_LETTER_EXCHANGE
                )
                .deadLetterRoutingKey(
                        RabbitMqConstants.REQUESTED_DEAD_KEY
                )
                .build();
    }

    @Bean
    Binding crawlRequestedBinding(
            @Qualifier("crawlRequestedQueue") Queue crawlRequestedQueue,
            @Qualifier("crawlExchange") DirectExchange crawlExchange
    ) {
        return BindingBuilder
                .bind(crawlRequestedQueue)
                .to(crawlExchange)
                .with(RabbitMqConstants.REQUESTED_KEY);
    }

    @Bean
    Queue crawlCompletedQueue() {
        return QueueBuilder
                .durable(RabbitMqConstants.COMPLETED_QUEUE)
                .deadLetterExchange(
                        RabbitMqConstants.DEAD_LETTER_EXCHANGE
                )
                .deadLetterRoutingKey(
                        RabbitMqConstants.COMPLETED_DEAD_KEY
                )
                .build();
    }

    @Bean
    Binding crawlCompletedBinding(
            @Qualifier("crawlCompletedQueue") Queue crawlCompletedQueue,
            @Qualifier("crawlExchange") DirectExchange crawlExchange
    ) {
        return BindingBuilder
                .bind(crawlCompletedQueue)
                .to(crawlExchange)
                .with(RabbitMqConstants.COMPLETED_KEY);
    }

    @Bean
    Queue crawlFailedQueue() {
        return QueueBuilder
                .durable(RabbitMqConstants.FAILED_QUEUE)
                .deadLetterExchange(
                        RabbitMqConstants.DEAD_LETTER_EXCHANGE
                )
                .deadLetterRoutingKey(
                        RabbitMqConstants.FAILED_DEAD_KEY
                )
                .build();
    }

    @Bean
    Binding crawlFailedBinding(
            @Qualifier("crawlFailedQueue") Queue crawlFailedQueue,
            @Qualifier("crawlExchange") DirectExchange crawlExchange
    ) {
        return BindingBuilder
                .bind(crawlFailedQueue)
                .to(crawlExchange)
                .with(RabbitMqConstants.FAILED_KEY);
    }

    @Bean
    Queue crawlRequestedDeadLetterQueue() {
        return QueueBuilder
                .durable(RabbitMqConstants.REQUESTED_DLQ)
                .build();
    }

    @Bean
    Binding crawlRequestedDeadLetterBinding(
            @Qualifier("crawlRequestedDeadLetterQueue") Queue queue,
            @Qualifier("crawlDeadLetterExchange") DirectExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(RabbitMqConstants.REQUESTED_DEAD_KEY);
    }

    @Bean
    Queue crawlCompletedDeadLetterQueue() {
        return QueueBuilder
                .durable(RabbitMqConstants.COMPLETED_DLQ)
                .build();
    }

    @Bean
    Binding crawlCompletedDeadLetterBinding(
            @Qualifier("crawlCompletedDeadLetterQueue") Queue queue,
            @Qualifier("crawlDeadLetterExchange") DirectExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(RabbitMqConstants.COMPLETED_DEAD_KEY);
    }

    @Bean
    Queue crawlFailedDeadLetterQueue() {
        return QueueBuilder
                .durable(RabbitMqConstants.FAILED_DLQ)
                .build();
    }

    @Bean
    Binding crawlFailedDeadLetterBinding(
            @Qualifier("crawlFailedDeadLetterQueue") Queue queue,
            @Qualifier("crawlDeadLetterExchange") DirectExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(RabbitMqConstants.FAILED_DEAD_KEY);
    }
    @Bean
    DirectExchange designAnalysisExchange() {
        return new DirectExchange(
                RabbitMqConstants.ANALYSIS_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    DirectExchange designAnalysisDeadLetterExchange() {
        return new DirectExchange(
                RabbitMqConstants.ANALYSIS_DEAD_LETTER_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    Queue designAnalysisRequestedQueue() {
        return QueueBuilder
                .durable(
                        RabbitMqConstants.ANALYSIS_REQUESTED_QUEUE
                )
                .deadLetterExchange(
                        RabbitMqConstants.ANALYSIS_DEAD_LETTER_EXCHANGE
                )
                .deadLetterRoutingKey(
                        RabbitMqConstants.ANALYSIS_REQUESTED_DEAD_KEY
                )
                .build();
    }

    @Bean
    Binding designAnalysisRequestedBinding(
            @Qualifier("designAnalysisRequestedQueue")
            Queue queue,

            @Qualifier("designAnalysisExchange")
            DirectExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(RabbitMqConstants.ANALYSIS_REQUESTED_KEY);
    }

    @Bean
    Queue designAnalysisRequestedDeadLetterQueue() {
        return QueueBuilder
                .durable(
                        RabbitMqConstants.ANALYSIS_REQUESTED_DLQ
                )
                .build();
    }

    @Bean
    Binding designAnalysisRequestedDeadLetterBinding(
            @Qualifier("designAnalysisRequestedDeadLetterQueue")
            Queue queue,

            @Qualifier("designAnalysisDeadLetterExchange")
            DirectExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(
                        RabbitMqConstants.ANALYSIS_REQUESTED_DEAD_KEY
                );
    }
    @Bean
    Queue designAnalysisCompletedQueue() {
        return QueueBuilder
                .durable(
                        RabbitMqConstants.ANALYSIS_COMPLETED_QUEUE
                )
                .deadLetterExchange(
                        RabbitMqConstants.ANALYSIS_DEAD_LETTER_EXCHANGE
                )
                .deadLetterRoutingKey(
                        RabbitMqConstants.ANALYSIS_COMPLETED_DEAD_KEY
                )
                .build();
    }

    @Bean
    Binding designAnalysisCompletedBinding(
            @Qualifier("designAnalysisCompletedQueue")
            Queue queue,

            @Qualifier("designAnalysisExchange")
            DirectExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(RabbitMqConstants.ANALYSIS_COMPLETED_KEY);
    }

    @Bean
    Queue designAnalysisCompletedDeadLetterQueue() {
        return QueueBuilder
                .durable(
                        RabbitMqConstants.ANALYSIS_COMPLETED_DLQ
                )
                .build();
    }

    @Bean
    Binding designAnalysisCompletedDeadLetterBinding(
            @Qualifier("designAnalysisCompletedDeadLetterQueue")
            Queue queue,

            @Qualifier("designAnalysisDeadLetterExchange")
            DirectExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(
                        RabbitMqConstants.ANALYSIS_COMPLETED_DEAD_KEY
                );
    }

    @Bean
    MessageConverter rabbitMessageConverter(
            ObjectMapper objectMapper
    ) {
        return new Jackson2JsonMessageConverter(
                objectMapper
        );
    }
}
