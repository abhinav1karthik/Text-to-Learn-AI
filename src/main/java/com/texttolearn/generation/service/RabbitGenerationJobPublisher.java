package com.texttolearn.generation.service;

import com.texttolearn.generation.config.GenerationRabbitMqConfig;
import com.texttolearn.generation.dto.GenerationJobMessage;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class RabbitGenerationJobPublisher implements GenerationJobPublisher {

    private final RabbitTemplate generationRabbitTemplate;
    private final GenerationJobTransitionService generationJobTransitionService;

    public RabbitGenerationJobPublisher(
            RabbitTemplate generationRabbitTemplate,
            GenerationJobTransitionService generationJobTransitionService
    ) {
        this.generationRabbitTemplate = generationRabbitTemplate;
        this.generationJobTransitionService = generationJobTransitionService;
    }

    @Override
    public void publishCourseGenerationJob(UUID jobId) {
        generationRabbitTemplate.convertAndSend(
                GenerationRabbitMqConfig.GENERATION_EXCHANGE,
                GenerationRabbitMqConfig.COURSE_GENERATION_ROUTING_KEY,
                new GenerationJobMessage(jobId)
        );
        generationJobTransitionService.markPublished(jobId);
    }
}
