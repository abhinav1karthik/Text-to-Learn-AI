package com.texttolearn.generation.service;

import com.rabbitmq.client.Channel;
import com.texttolearn.generation.config.GenerationRabbitMqConfig;
import com.texttolearn.generation.dto.GenerationJobMessage;
import java.io.IOException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.rabbitmq.listeners.enabled", havingValue = "true", matchIfMissing = true)
public class CourseGenerationJobListener {

    private final GenerationJobWorker generationJobWorker;

    public CourseGenerationJobListener(GenerationJobWorker generationJobWorker) {
        this.generationJobWorker = generationJobWorker;
    }

    @RabbitListener(
            queues = GenerationRabbitMqConfig.COURSE_GENERATION_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleCourseGenerationJob(
            GenerationJobMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        try {
            generationJobWorker.processCourseGenerationJob(message.jobId());
            channel.basicAck(deliveryTag, false);
        } catch (RuntimeException exception) {
            channel.basicNack(deliveryTag, false, false);
            throw exception;
        }
    }
}
