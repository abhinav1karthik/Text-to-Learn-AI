package com.texttolearn.generation.service;

import com.texttolearn.common.error.ResourceNotFoundException;
import com.texttolearn.generation.dto.GenerationJobResponse;
import com.texttolearn.generation.model.GenerationJob;
import com.texttolearn.generation.model.GenerationJobPriority;
import com.texttolearn.generation.model.GenerationJobStatus;
import com.texttolearn.generation.model.GenerationJobType;
import com.texttolearn.generation.repository.GenerationJobRepository;
import com.texttolearn.user.model.AppUser;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class GenerationJobService {

    private static final List<GenerationJobStatus> ACTIVE_JOB_STATUSES = List.of(
            GenerationJobStatus.QUEUED,
            GenerationJobStatus.RUNNING
    );

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
        return generationJobRepository
                .findFirstByUserAndTypeAndStatusInOrderByCreatedAtDesc(
                        user,
                        GenerationJobType.COURSE_OUTLINE,
                        ACTIVE_JOB_STATUSES
                )
                .map(this::toResponse)
                .orElseGet(() -> createNewCourseGenerationJob(user, topic));
    }

    private GenerationJobResponse createNewCourseGenerationJob(AppUser user, String topic) {
        GenerationJob job = new GenerationJob(
                user,
                GenerationJobType.COURSE_OUTLINE,
                GenerationJobPriority.NORMAL,
                topic.trim(),
                null
        );
        GenerationJob savedJob = generationJobRepository.saveAndFlush(job);
        scheduleAfterCommit(savedJob.getId());
        return toResponse(savedJob);
    }

    @Transactional
    public GenerationJobResponse createLessonGenerationJob(
            AppUser user,
            UUID lessonId,
            String lessonTitle,
            GenerationJobPriority priority
    ) {
        return generationJobRepository
                .findFirstByLessonIdAndTypeAndStatusInOrderByCreatedAtDesc(
                        lessonId,
                        GenerationJobType.LESSON_CONTENT,
                        ACTIVE_JOB_STATUSES
                )
                .map(this::toResponse)
                .orElseGet(() -> createNewLessonGenerationJob(user, lessonId, lessonTitle, priority));
    }

    private GenerationJobResponse createNewLessonGenerationJob(
            AppUser user,
            UUID lessonId,
            String lessonTitle,
            GenerationJobPriority priority
    ) {
        GenerationJob job = new GenerationJob(
                user,
                GenerationJobType.LESSON_CONTENT,
                priority,
                lessonTitle.trim(),
                lessonId
        );
        GenerationJob savedJob = generationJobRepository.saveAndFlush(job);
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
                job.getPriority(),
                job.getPrompt(),
                job.getCourseId(),
                job.getLessonId(),
                job.getErrorMessage(),
                job.getLastErrorType(),
                job.getAttemptCount(),
                job.getMaxAttempts(),
                job.getNextRunAt(),
                job.getLockedAt(),
                job.getLockedBy(),
                job.getLastPublishedAt(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getStartedAt(),
                job.getCompletedAt()
        );
    }
}
