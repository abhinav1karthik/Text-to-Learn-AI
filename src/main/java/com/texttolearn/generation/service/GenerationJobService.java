package com.texttolearn.generation.service;

import com.texttolearn.common.error.ResourceNotFoundException;
import com.texttolearn.generation.dto.GenerationJobResponse;
import com.texttolearn.generation.model.GenerationJob;
import com.texttolearn.generation.model.GenerationJobType;
import com.texttolearn.generation.repository.GenerationJobRepository;
import com.texttolearn.user.model.AppUser;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class GenerationJobService {

    private final GenerationJobRepository generationJobRepository;
    private final GenerationJobWorker generationJobWorker;

    public GenerationJobService(
            GenerationJobRepository generationJobRepository,
            GenerationJobWorker generationJobWorker
    ) {
        this.generationJobRepository = generationJobRepository;
        this.generationJobWorker = generationJobWorker;
    }

    @Transactional
    public GenerationJobResponse createCourseGenerationJob(AppUser user, String topic) {
        GenerationJob job = new GenerationJob(
                user,
                GenerationJobType.COURSE_OUTLINE,
                topic.trim()
        );
        GenerationJob savedJob = generationJobRepository.saveAndFlush(job);
        scheduleAfterCommit(savedJob.getId());
        return toResponse(savedJob);
    }

    @Transactional(readOnly = true)
    public GenerationJobResponse getJobForUser(AppUser user, UUID jobId) {
        GenerationJob job = generationJobRepository.findByIdAndUser(jobId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Generation job not found"));
        return toResponse(job);
    }

    private void scheduleAfterCommit(UUID jobId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            generationJobWorker.processCourseGenerationJob(jobId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                generationJobWorker.processCourseGenerationJob(jobId);
            }
        });
    }

    private GenerationJobResponse toResponse(GenerationJob job) {
        return new GenerationJobResponse(
                job.getId(),
                job.getType(),
                job.getStatus(),
                job.getPrompt(),
                job.getCourseId(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getStartedAt(),
                job.getCompletedAt()
        );
    }
}
