package com.texttolearn.generation.service;

import com.texttolearn.generation.model.GenerationJobErrorType;
import com.texttolearn.generation.model.GenerationJobStatus;
import com.texttolearn.generation.model.GenerationJobType;
import com.texttolearn.generation.repository.GenerationJobRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.generation.recovery.enabled", havingValue = "true", matchIfMissing = true)
public class GenerationJobRecoveryScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerationJobRecoveryScheduler.class);
    private static final int MAX_JOBS_PER_SCAN = 25;
    private static final int COURSE_TIMEOUT_MINUTES = 20;
    private static final int LESSON_TIMEOUT_MINUTES = 10;

    private final GenerationJobRepository generationJobRepository;
    private final GenerationJobTransitionService generationJobTransitionService;

    public GenerationJobRecoveryScheduler(
            GenerationJobRepository generationJobRepository,
            GenerationJobTransitionService generationJobTransitionService
    ) {
        this.generationJobRepository = generationJobRepository;
        this.generationJobTransitionService = generationJobTransitionService;
    }

    @Scheduled(fixedDelayString = "${app.generation.recovery.fixed-delay-ms:60000}")
    public void recoverStaleRunningJobs() {
        recoverStaleRunningJobs(GenerationJobType.COURSE_OUTLINE, COURSE_TIMEOUT_MINUTES);
        recoverStaleRunningJobs(GenerationJobType.LESSON_CONTENT, LESSON_TIMEOUT_MINUTES);
    }

    private void recoverStaleRunningJobs(GenerationJobType type, int timeoutMinutes) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime staleLockedBefore = now.minusMinutes(timeoutMinutes);
        List<UUID> staleJobIds = generationJobRepository.findStaleRunningJobIds(
                type,
                GenerationJobStatus.RUNNING,
                staleLockedBefore,
                PageRequest.of(0, MAX_JOBS_PER_SCAN)
        );

        for (UUID jobId : staleJobIds) {
            Optional<GenerationJobStatus> recoveredStatus = generationJobTransitionService.recoverStaleRunningJob(
                    jobId,
                    type,
                    staleLockedBefore,
                    staleWorkerMessage(type, timeoutMinutes),
                    GenerationJobErrorType.UNKNOWN,
                    now
            );

            recoveredStatus.ifPresent(status -> logRecovery(jobId, type, status));
        }
    }

    private String staleWorkerMessage(GenerationJobType type, int timeoutMinutes) {
        return "%s job recovered after being RUNNING for more than %d minutes."
                .formatted(type.name(), timeoutMinutes);
    }

    private void logRecovery(UUID jobId, GenerationJobType type, GenerationJobStatus status) {
        if (status == GenerationJobStatus.QUEUED) {
            LOGGER.info("Recovered stale {} generation job {} back to QUEUED", type, jobId);
            return;
        }

        if (status == GenerationJobStatus.FAILED) {
            LOGGER.warn("Marked stale {} generation job {} as FAILED after attempts were exhausted", type, jobId);
        }
    }
}
