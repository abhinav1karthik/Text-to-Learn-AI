package com.texttolearn.generation.service;

import com.texttolearn.course.dto.CourseResponse;
import com.texttolearn.course.dto.LessonResponse;
import com.texttolearn.course.dto.LessonSummaryResponse;
import com.texttolearn.course.model.LessonStatus;
import com.texttolearn.course.service.CourseService;
import com.texttolearn.generation.model.GenerationJob;
import com.texttolearn.generation.model.GenerationJobPriority;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GenerationJobWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerationJobWorker.class);
    private static final int MAX_ERROR_MESSAGE_LENGTH = 2000;
    private static final int LOW_PRIORITY_PREGENERATION_LIMIT = 2;

    private final String workerId = "spring-worker-" + UUID.randomUUID();

    private final GenerationJobTransitionService generationJobTransitionService;
    private final CourseService courseService;
    private final GenerationJobService generationJobService;
    private final GenerationJobFailureClassifier failureClassifier;
    private final GenerationJobRetryPolicy retryPolicy;

    public GenerationJobWorker(
            GenerationJobTransitionService generationJobTransitionService,
            CourseService courseService,
            GenerationJobService generationJobService,
            GenerationJobFailureClassifier failureClassifier,
            GenerationJobRetryPolicy retryPolicy
    ) {
        this.generationJobTransitionService = generationJobTransitionService;
        this.courseService = courseService;
        this.generationJobService = generationJobService;
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
                boolean markedSucceeded = generationJobTransitionService.markSucceeded(jobId, course.id(), lockedBy);
                if (markedSucceeded) {
                    enqueueLowPriorityLessons(job, course);
                }
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

    private void enqueueLowPriorityLessons(GenerationJob job, CourseResponse course) {
        try {
            course.modules().stream()
                    .flatMap(module -> module.lessons().stream())
                    .filter(lesson -> lesson.status() == LessonStatus.PLANNED)
                    .limit(LOW_PRIORITY_PREGENERATION_LIMIT)
                    .forEach(lesson -> createLowPriorityLessonJob(job, lesson));
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Course {} was generated, but low-priority lesson pre-generation could not be queued.",
                    course.id(),
                    exception
            );
        }
    }

    private void createLowPriorityLessonJob(GenerationJob job, LessonSummaryResponse lesson) {
        generationJobService.createLessonGenerationJob(
                job.getUser(),
                lesson.id(),
                lesson.title(),
                GenerationJobPriority.LOW
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
