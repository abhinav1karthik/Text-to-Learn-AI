package com.texttolearn.generation.service;

import com.texttolearn.generation.model.GenerationJobStatus;
import com.texttolearn.generation.model.GenerationJobType;
import com.texttolearn.generation.repository.GenerationJobRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.generation.republisher.enabled", havingValue = "true", matchIfMissing = true)
public class GenerationJobRepublisherScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerationJobRepublisherScheduler.class);
    private static final int MAX_JOBS_PER_SCAN = 25;
    private static final int STALE_PUBLISH_SECONDS = 30;

    private final GenerationJobRepository generationJobRepository;
    private final GenerationJobPublisher generationJobPublisher;

    public GenerationJobRepublisherScheduler(
            GenerationJobRepository generationJobRepository,
            GenerationJobPublisher generationJobPublisher
    ) {
        this.generationJobRepository = generationJobRepository;
        this.generationJobPublisher = generationJobPublisher;
    }

    @Scheduled(fixedDelayString = "${app.generation.republisher.fixed-delay-ms:5000}")
    public void republishDueCourseJobs() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime stalePublishedBefore = now.minusSeconds(STALE_PUBLISH_SECONDS);
        List<UUID> dueJobIds = generationJobRepository.findDueQueuedJobIdsForPublishing(
                GenerationJobType.COURSE_OUTLINE,
                GenerationJobStatus.QUEUED,
                now,
                stalePublishedBefore,
                PageRequest.of(0, MAX_JOBS_PER_SCAN)
        );

        for (UUID jobId : dueJobIds) {
            try {
                generationJobPublisher.publishCourseGenerationJob(jobId);
            } catch (RuntimeException exception) {
                LOGGER.warn("Failed to republish generation job {}", jobId, exception);
            }
        }
    }
}
