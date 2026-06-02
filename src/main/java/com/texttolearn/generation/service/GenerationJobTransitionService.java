package com.texttolearn.generation.service;

import com.texttolearn.generation.model.GenerationJob;
import com.texttolearn.generation.model.GenerationJobErrorType;
import com.texttolearn.generation.repository.GenerationJobRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenerationJobTransitionService {

    private final GenerationJobRepository generationJobRepository;

    public GenerationJobTransitionService(GenerationJobRepository generationJobRepository) {
        this.generationJobRepository = generationJobRepository;
    }

    @Transactional
    public boolean claim(UUID jobId, String lockedBy) {
        return generationJobRepository.claimQueuedJob(jobId, lockedBy) == 1;
    }

    @Transactional(readOnly = true)
    public Optional<GenerationJob> getClaimedJobWithUser(UUID jobId, String lockedBy) {
        return generationJobRepository.findByIdWithUserAndLockedBy(jobId, lockedBy)
                .filter(job -> job.isRunningLockedBy(lockedBy));
    }

    @Transactional
    public boolean markSucceeded(UUID jobId, UUID courseId, String lockedBy) {
        return generationJobRepository.findById(jobId)
                .filter(job -> job.isRunningLockedBy(lockedBy))
                .map(job -> {
                    job.markSucceeded(courseId);
                    generationJobRepository.saveAndFlush(job);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public boolean markRetryQueued(
            UUID jobId,
            String errorMessage,
            GenerationJobErrorType errorType,
            OffsetDateTime nextRunAt,
            String lockedBy
    ) {
        return generationJobRepository.findById(jobId)
                .filter(job -> job.isRunningLockedBy(lockedBy))
                .map(job -> {
                    job.markRetryQueued(errorMessage, errorType, nextRunAt);
                    generationJobRepository.saveAndFlush(job);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public boolean markFailed(
            UUID jobId,
            String errorMessage,
            GenerationJobErrorType errorType,
            String lockedBy
    ) {
        return generationJobRepository.findById(jobId)
                .filter(job -> job.isRunningLockedBy(lockedBy))
                .map(job -> {
                    job.markFailed(errorMessage, errorType);
                    generationJobRepository.saveAndFlush(job);
                    return true;
                })
                .orElse(false);
    }
}
