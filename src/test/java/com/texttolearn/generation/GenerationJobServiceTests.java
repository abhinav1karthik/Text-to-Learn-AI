package com.texttolearn.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.texttolearn.ai.dto.GeneratedCourseOutline;
import com.texttolearn.ai.dto.GeneratedLessonContent;
import com.texttolearn.ai.dto.GeneratedModuleOutline;
import com.texttolearn.ai.error.AiGenerationException;
import com.texttolearn.ai.service.CourseAiService;
import com.texttolearn.common.error.ResourceNotFoundException;
import com.texttolearn.course.dto.CourseResponse;
import com.texttolearn.course.repository.CourseRepository;
import com.texttolearn.course.service.CourseService;
import com.texttolearn.generation.dto.GenerationJobResponse;
import com.texttolearn.generation.model.GenerationJob;
import com.texttolearn.generation.model.GenerationJobErrorType;
import com.texttolearn.generation.model.GenerationJobStatus;
import com.texttolearn.generation.model.GenerationJobType;
import com.texttolearn.generation.repository.GenerationJobRepository;
import com.texttolearn.generation.service.GenerationJobPublisher;
import com.texttolearn.generation.service.GenerationJobService;
import com.texttolearn.generation.service.GenerationJobTransitionService;
import com.texttolearn.generation.service.GenerationJobWorker;
import com.texttolearn.user.model.AppUser;
import com.texttolearn.user.repository.AppUserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest
class GenerationJobServiceTests {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private GenerationJobRepository generationJobRepository;

    @Autowired
    private GenerationJobService generationJobService;

    @Autowired
    private GenerationJobTransitionService generationJobTransitionService;

    @Autowired
    private GenerationJobWorker generationJobWorker;

    @Autowired
    private CourseService courseService;

    @Autowired
    private CapturingGenerationJobPublisher generationJobPublisher;

    @Autowired
    private TestCourseAiService courseAiService;

    @BeforeEach
    void resetFakes() {
        courseAiService.reset();
        generationJobPublisher.clear();
    }

    @Test
    void createsCourseGenerationJobAndPublishesRabbitMessageAfterCommit() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|generation-job-user", "job@example.com", "Job Student", null)
        );

        GenerationJobResponse queuedJob = generationJobService.createCourseGenerationJob(
                user,
                "Segment Trees and Their Applications"
        );

        assertThat(queuedJob.id()).isNotNull();
        assertThat(queuedJob.status()).isEqualTo(GenerationJobStatus.QUEUED);
        assertThat(queuedJob.courseId()).isNull();
        assertThat(generationJobPublisher.publishedJobIds()).contains(queuedJob.id());

        GenerationJob persistedJob = generationJobRepository.findById(queuedJob.id()).orElseThrow();
        assertThat(persistedJob.getStatus()).isEqualTo(GenerationJobStatus.QUEUED);
        assertThat(persistedJob.getLastPublishedAt()).isNotNull();
    }

    @Test
    void courseWorkerConsumesPublishedJobAndMarksItSucceeded() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|course-worker-user", "course-worker@example.com", "Worker Student", null)
        );
        GenerationJobResponse queuedJob = generationJobService.createCourseGenerationJob(
                user,
                "Segment Trees and Their Applications"
        );

        generationJobWorker.processCourseGenerationJob(queuedJob.id());

        GenerationJobResponse completedJob = generationJobService.getJobForUser(user, queuedJob.id());
        assertThat(completedJob.status()).isEqualTo(GenerationJobStatus.SUCCEEDED);
        assertThat(completedJob.courseId()).isNotNull();
        assertThat(completedJob.completedAt()).isNotNull();
        assertThat(completedJob.attemptCount()).isEqualTo(1);
        assertThat(courseRepository.findByIdAndUser(completedJob.courseId(), user)).isPresent();
    }

    @Test
    void duplicateCourseMessageDoesNotCreateDuplicateCourse() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|duplicate-message-user", "duplicate@example.com", "Duplicate Student", null)
        );
        GenerationJobResponse queuedJob = generationJobService.createCourseGenerationJob(
                user,
                "Duplicate Course Message"
        );

        long before = courseRepository.count();
        generationJobWorker.processCourseGenerationJob(queuedJob.id());
        GenerationJobResponse firstResult = generationJobService.getJobForUser(user, queuedJob.id());
        generationJobWorker.processCourseGenerationJob(queuedJob.id());
        GenerationJobResponse secondResult = generationJobService.getJobForUser(user, queuedJob.id());

        assertThat(firstResult.status()).isEqualTo(GenerationJobStatus.SUCCEEDED);
        assertThat(secondResult.status()).isEqualTo(GenerationJobStatus.SUCCEEDED);
        assertThat(secondResult.courseId()).isEqualTo(firstResult.courseId());
        assertThat(secondResult.attemptCount()).isEqualTo(1);
        assertThat(courseRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void queuedJobWithExistingCourseReusesCourseWhenMessageIsRedelivered() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|existing-course-user", "existing-course@example.com", "Existing Student", null)
        );
        GenerationJob job = generationJobRepository.saveAndFlush(
                new GenerationJob(user, GenerationJobType.COURSE_OUTLINE, "Existing course retry")
        );
        CourseResponse existingCourse = courseService.createCourseForGenerationJob(
                user,
                job.getPrompt(),
                job.getId()
        );
        long courseCount = courseRepository.count();

        generationJobWorker.processCourseGenerationJob(job.getId());

        GenerationJob completedJob = generationJobRepository.findById(job.getId()).orElseThrow();
        assertThat(completedJob.getStatus()).isEqualTo(GenerationJobStatus.SUCCEEDED);
        assertThat(completedJob.getCourseId()).isEqualTo(existingCourse.id());
        assertThat(completedJob.getAttemptCount()).isEqualTo(1);
        assertThat(courseRepository.count()).isEqualTo(courseCount);
    }

    @Test
    void retryableRateLimitFailureIsRequeuedWithBackoff() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|rate-limit-user", "rate-limit@example.com", "Rate Limit Student", null)
        );
        courseAiService.failCourseOutlineWith(new AiGenerationException(
                "Gemini request failed with status 429: RESOURCE_EXHAUSTED rate limit"
        ));
        GenerationJobResponse queuedJob = generationJobService.createCourseGenerationJob(
                user,
                "Rate Limited Course"
        );

        OffsetDateTime beforeProcessing = OffsetDateTime.now();
        generationJobWorker.processCourseGenerationJob(queuedJob.id());

        GenerationJob retryJob = generationJobRepository.findById(queuedJob.id()).orElseThrow();
        assertThat(retryJob.getStatus()).isEqualTo(GenerationJobStatus.QUEUED);
        assertThat(retryJob.getLastErrorType()).isEqualTo(GenerationJobErrorType.AI_RATE_LIMIT);
        assertThat(retryJob.getErrorMessage()).contains("429");
        assertThat(retryJob.getAttemptCount()).isEqualTo(1);
        assertThat(retryJob.getLockedBy()).isNull();
        assertThat(retryJob.getLockedAt()).isNull();
        assertThat(retryJob.getCompletedAt()).isNull();
        assertThat(retryJob.getLastPublishedAt()).isNull();
        assertThat(retryJob.getNextRunAt()).isAfterOrEqualTo(beforeProcessing.plusSeconds(14));
        assertThat(retryJob.getNextRunAt()).isBeforeOrEqualTo(beforeProcessing.plusSeconds(20));
    }

    @Test
    void retryableBadAiResponseIsRequeued() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|bad-ai-user", "bad-ai@example.com", "Bad AI Student", null)
        );
        courseAiService.failCourseOutlineWith(new AiGenerationException(
                "Gemini returned invalid course outline JSON."
        ));
        GenerationJobResponse queuedJob = generationJobService.createCourseGenerationJob(
                user,
                "Bad AI Response Course"
        );

        generationJobWorker.processCourseGenerationJob(queuedJob.id());

        GenerationJob retryJob = generationJobRepository.findById(queuedJob.id()).orElseThrow();
        assertThat(retryJob.getStatus()).isEqualTo(GenerationJobStatus.QUEUED);
        assertThat(retryJob.getLastErrorType()).isEqualTo(GenerationJobErrorType.AI_BAD_RESPONSE);
        assertThat(retryJob.getAttemptCount()).isEqualTo(1);
        assertThat(retryJob.getCompletedAt()).isNull();
    }

    @Test
    void permanentNotFoundFailureFailsFast() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|permanent-failure-user", "permanent@example.com", "Permanent Student", null)
        );
        courseAiService.failCourseOutlineWith(new ResourceNotFoundException("Course source not found"));
        GenerationJobResponse queuedJob = generationJobService.createCourseGenerationJob(
                user,
                "Permanent Failure Course"
        );

        generationJobWorker.processCourseGenerationJob(queuedJob.id());

        GenerationJob failedJob = generationJobRepository.findById(queuedJob.id()).orElseThrow();
        assertThat(failedJob.getStatus()).isEqualTo(GenerationJobStatus.FAILED);
        assertThat(failedJob.getLastErrorType()).isEqualTo(GenerationJobErrorType.NOT_FOUND);
        assertThat(failedJob.getErrorMessage()).isEqualTo("Course source not found");
        assertThat(failedJob.getAttemptCount()).isEqualTo(1);
        assertThat(failedJob.getCompletedAt()).isNotNull();
        assertThat(failedJob.getLockedBy()).isNull();
    }

    @Test
    void atomicallyClaimsQueuedJobOnlyOnce() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|atomic-claim-user", "atomic@example.com", "Atomic Student", null)
        );
        GenerationJob job = generationJobRepository.saveAndFlush(
                new GenerationJob(user, GenerationJobType.COURSE_OUTLINE, "Atomic job claim")
        );

        boolean firstClaimed = generationJobTransitionService.claim(job.getId(), "worker-a");
        boolean secondClaimed = generationJobTransitionService.claim(job.getId(), "worker-b");

        assertThat(firstClaimed).isTrue();
        assertThat(secondClaimed).isFalse();

        GenerationJob claimedJob = generationJobRepository.findById(job.getId()).orElseThrow();
        assertThat(claimedJob.getStatus()).isEqualTo(GenerationJobStatus.RUNNING);
        assertThat(claimedJob.getAttemptCount()).isEqualTo(1);
        assertThat(claimedJob.getLockedBy()).isEqualTo("worker-a");
        assertThat(claimedJob.getLockedAt()).isNotNull();
        assertThat(claimedJob.getStartedAt()).isNotNull();
    }

    @Test
    void onlyLockOwnerCanTransitionClaimedJob() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|claim-transition-user", "claim-transition@example.com", "Claim Student", null)
        );
        GenerationJob job = generationJobRepository.saveAndFlush(
                new GenerationJob(user, GenerationJobType.COURSE_OUTLINE, "Transition claim")
        );
        generationJobTransitionService.claim(job.getId(), "worker-a");

        boolean wrongWorkerFailed = generationJobTransitionService.markFailed(
                job.getId(),
                "Wrong worker",
                GenerationJobErrorType.UNKNOWN,
                "worker-b"
        );
        assertThat(wrongWorkerFailed).isFalse();

        OffsetDateTime nextRunAt = OffsetDateTime.now().plusMinutes(1);
        boolean retryQueued = generationJobTransitionService.markRetryQueued(
                job.getId(),
                "Retry later",
                GenerationJobErrorType.AI_TIMEOUT,
                nextRunAt,
                "worker-a"
        );

        assertThat(retryQueued).isTrue();

        GenerationJob retryJob = generationJobRepository.findById(job.getId()).orElseThrow();
        assertThat(retryJob.getStatus()).isEqualTo(GenerationJobStatus.QUEUED);
        assertThat(retryJob.getAttemptCount()).isEqualTo(1);
        assertThat(retryJob.getLockedBy()).isNull();
        assertThat(retryJob.getLockedAt()).isNull();
        assertThat(retryJob.getLastErrorType()).isEqualTo(GenerationJobErrorType.AI_TIMEOUT);

        assertThat(generationJobTransitionService.claim(job.getId(), "worker-b")).isFalse();
    }

    @TestConfiguration
    static class FakeAiConfig {

        @Bean
        @Primary
        CapturingGenerationJobPublisher capturingGenerationJobPublisher(
                GenerationJobTransitionService generationJobTransitionService
        ) {
            return new CapturingGenerationJobPublisher(generationJobTransitionService);
        }

        @Bean
        @Primary
        TestCourseAiService fakeCourseAiService() {
            return new TestCourseAiService();
        }
    }

    static class TestCourseAiService implements CourseAiService {

        private RuntimeException courseOutlineException;

        void reset() {
            courseOutlineException = null;
        }

        void failCourseOutlineWith(RuntimeException exception) {
            courseOutlineException = exception;
        }

        @Override
        public GeneratedCourseOutline generateCourseOutline(String topic) {
            if (courseOutlineException != null) {
                throw courseOutlineException;
            }

            return new GeneratedCourseOutline(
                    "Async Segment Trees",
                    "A generated outline from a background job.",
                    List.of("Algorithms"),
                    List.of(new GeneratedModuleOutline(
                            "Foundations",
                            "Core segment tree concepts.",
                            List.of("What is a Segment Tree?")
                    ))
            );
        }

        @Override
        public GeneratedLessonContent generateLessonContent(
                String courseTitle,
                String moduleTitle,
                String lessonTitle
        ) {
            return new GeneratedLessonContent(
                    lessonTitle,
                    List.of("Understand the lesson"),
                    List.of(Map.of("type", "paragraph", "text", "Generated lesson content."))
            );
        }
    }

    static class CapturingGenerationJobPublisher implements GenerationJobPublisher {

        private final GenerationJobTransitionService generationJobTransitionService;
        private final List<UUID> publishedJobIds = new CopyOnWriteArrayList<>();

        CapturingGenerationJobPublisher(GenerationJobTransitionService generationJobTransitionService) {
            this.generationJobTransitionService = generationJobTransitionService;
        }

        @Override
        public void publishCourseGenerationJob(UUID jobId) {
            publishedJobIds.add(jobId);
            generationJobTransitionService.markPublished(jobId);
        }

        void clear() {
            publishedJobIds.clear();
        }

        List<UUID> publishedJobIds() {
            return publishedJobIds;
        }
    }
}
