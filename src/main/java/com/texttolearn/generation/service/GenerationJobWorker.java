package com.texttolearn.generation.service;

import com.texttolearn.course.dto.CourseResponse;
import com.texttolearn.course.service.CourseService;
import com.texttolearn.generation.model.GenerationJob;
import com.texttolearn.generation.model.GenerationJobErrorType;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class GenerationJobWorker {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 2000;

    private final String workerId = "spring-worker-" + UUID.randomUUID();

    private final GenerationJobTransitionService generationJobTransitionService;
    private final CourseService courseService;

    public GenerationJobWorker(
            GenerationJobTransitionService generationJobTransitionService,
            CourseService courseService
    ) {
        this.generationJobTransitionService = generationJobTransitionService;
        this.courseService = courseService;
    }

    @Async("generationTaskExecutor")
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
                generationJobTransitionService.markFailed(
                        jobId,
                        safeErrorMessage(exception),
                        GenerationJobErrorType.UNKNOWN,
                        lockedBy
                );
            }
        });
    }

    private String lockOwner() {
        return workerId + ":" + Thread.currentThread().getName();
    }

    private String safeErrorMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = "Course generation failed.";
        }

        if (message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return message;
        }

        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
