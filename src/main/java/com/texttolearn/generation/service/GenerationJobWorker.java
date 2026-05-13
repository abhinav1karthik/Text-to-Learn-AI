package com.texttolearn.generation.service;

import com.texttolearn.course.dto.CourseResponse;
import com.texttolearn.course.service.CourseService;
import com.texttolearn.generation.model.GenerationJob;
import com.texttolearn.generation.model.GenerationJobStatus;
import com.texttolearn.generation.repository.GenerationJobRepository;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class GenerationJobWorker {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 2000;

    private final GenerationJobRepository generationJobRepository;
    private final CourseService courseService;

    public GenerationJobWorker(
            GenerationJobRepository generationJobRepository,
            CourseService courseService
    ) {
        this.generationJobRepository = generationJobRepository;
        this.courseService = courseService;
    }

    @Async("generationTaskExecutor")
    public void processCourseGenerationJob(UUID jobId) {
        generationJobRepository.findByIdWithUser(jobId).ifPresent(job -> {
            if (job.getStatus() != GenerationJobStatus.QUEUED) {
                return;
            }

            try {
                markRunning(job);
                CourseResponse course = courseService.createCourse(job.getUser(), job.getPrompt());
                markSucceeded(jobId, course.id());
            } catch (RuntimeException exception) {
                markFailed(jobId, exception);
            }
        });
    }

    private void markRunning(GenerationJob job) {
        job.markRunning();
        generationJobRepository.saveAndFlush(job);
    }

    private void markSucceeded(UUID jobId, UUID courseId) {
        generationJobRepository.findById(jobId).ifPresent(job -> {
            job.markSucceeded(courseId);
            generationJobRepository.saveAndFlush(job);
        });
    }

    private void markFailed(UUID jobId, RuntimeException exception) {
        generationJobRepository.findById(jobId).ifPresent(job -> {
            job.markFailed(safeErrorMessage(exception));
            generationJobRepository.saveAndFlush(job);
        });
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
