package com.texttolearn.generation.service;

import com.texttolearn.generation.config.GenerationRabbitMqConfig;
import com.texttolearn.generation.dto.GenerationJobMessage;
import com.texttolearn.generation.model.GenerationJobPriority;
import com.texttolearn.generation.model.GenerationJobType;
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
    public void publishGenerationJob(UUID jobId, GenerationJobType type, GenerationJobPriority priority) {
        generationRabbitTemplate.convertAndSend(
                GenerationRabbitMqConfig.GENERATION_EXCHANGE,
                routingKey(type, priority),
                new GenerationJobMessage(jobId)
        );
        generationJobTransitionService.markPublished(jobId);
    }

    @Override
    public void publishCourseGenerationJob(UUID jobId) {
        publishGenerationJob(jobId, GenerationJobType.COURSE_OUTLINE, GenerationJobPriority.NORMAL);
    }

    private String routingKey(GenerationJobType type, GenerationJobPriority priority) {
        if (type == GenerationJobType.COURSE_OUTLINE) {
            return GenerationRabbitMqConfig.COURSE_GENERATION_ROUTING_KEY;
        }

        if (priority == GenerationJobPriority.HIGH) {
            return GenerationRabbitMqConfig.LESSON_GENERATION_HIGH_ROUTING_KEY;
        }

        return GenerationRabbitMqConfig.LESSON_GENERATION_LOW_ROUTING_KEY;
    }
}
