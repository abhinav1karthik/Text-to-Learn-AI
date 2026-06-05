package com.texttolearn.generation.service;

import com.texttolearn.course.dto.CourseResponse;
import com.texttolearn.course.dto.LessonResponse;
import com.texttolearn.course.service.CourseService;
import com.texttolearn.generation.model.GenerationJob;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GenerationJobWorker {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 2000;

    private final String workerId = "spring-worker-" + UUID.randomUUID();

    private final GenerationJobTransitionService generationJobTransitionService;
    private final CourseService courseService;
    private final GenerationJobFailureClassifier failureClassifier;
    private final GenerationJobRetryPolicy retryPolicy;

    public GenerationJobWorker(
            GenerationJobTransitionService generationJobTransitionService,
            CourseService courseService,
            GenerationJobFailureClassifier failureClassifier,
            GenerationJobRetryPolicy retryPolicy
    ) {
        this.generationJobTransitionService = generationJobTransitionService;
        this.courseService = courseService;
        this.failureClassifier = failureClassifier;
        this.retryPolicy = retryPolicy;
    }

    public void processCourseGenerationJob(UUID jobId) {
        String lockedBy = lockOwner();
        if (!generationJobTransitionService.claim(jobId, lockedBy)) {
            return;
        }

        generationJobTransitionService.getClaimedJobWithUser(jobId, lockedBy).ifPresent(job -> {
            try {
                CourseResponse course = courseService.createCourseForGenerationJob(
                        job.getUser(),
                        job.getPrompt(),
                        job.getId()
                );
                generationJobTransitionService.markSucceeded(jobId, course.id(), lockedBy);
            } catch (RuntimeException exception) {
                handleFailure(job, exception, lockedBy);
            }
        });
    }

    public void processLessonGenerationJob(UUID jobId) {
        String lockedBy = lockOwner();
        if (!generationJobTransitionService.claim(jobId, lockedBy)) {
            return;
        }

        generationJobTransitionService.getClaimedJobWithUser(jobId, lockedBy).ifPresent(job -> {
            try {
                LessonResponse lesson = courseService.generateLessonForGenerationJob(job.getUser(), job.getLessonId());
                generationJobTransitionService.markSucceeded(jobId, lesson.courseId(), lockedBy);
            } catch (RuntimeException exception) {
                handleFailure(job, exception, lockedBy);
            }
        });
    }

    private void handleFailure(GenerationJob job, RuntimeException exception, String lockedBy) {
        GenerationJobFailureDecision failure = failureClassifier.classify(exception);
        String errorMessage = safeErrorMessage(failure.message());

        if (retryPolicy.shouldRetry(failure, job.getAttemptCount(), job.getMaxAttempts())) {
            OffsetDateTime nextRunAt = OffsetDateTime.now().plus(retryPolicy.backoffForAttempt(job.getAttemptCount()));
            generationJobTransitionService.markRetryQueued(
                    job.getId(),
                    errorMessage,
                    failure.errorType(),
                    nextRunAt,
                    lockedBy
            );
            return;
        }

        generationJobTransitionService.markFailed(
                job.getId(),
                errorMessage,
                failure.errorType(),
                lockedBy
        );
    }

    private String lockOwner() {
        return workerId + ":" + Thread.currentThread().getName();
    }

    private String safeErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            message = "Course generation failed.";
        }

        if (message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return message;
        }

        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
