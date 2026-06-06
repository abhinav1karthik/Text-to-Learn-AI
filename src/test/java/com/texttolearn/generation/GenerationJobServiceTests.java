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
import com.texttolearn.generation.model.GenerationJobPriority;
import com.texttolearn.generation.model.GenerationJobStatus;
import com.texttolearn.generation.model.GenerationJobType;
import com.texttolearn.generation.repository.GenerationJobRepository;
import com.texttolearn.generation.service.GenerationJobRecoveryScheduler;
import com.texttolearn.generation.service.GenerationJobPublisher;
import com.texttolearn.generation.service.GenerationJobRepublisherScheduler;
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
import org.springframework.jdbc.core.JdbcTemplate;

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
    private JdbcTemplate jdbcTemplate;

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
    void courseWorkerEnqueuesOnlyFirstTwoLessonsAsLowPriorityPregenerationJobs() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|low-pregen-user", "low-pregen@example.com", "Low Pregen Student", null)
        );
        GenerationJobResponse queuedJob = generationJobService.createCourseGenerationJob(
                user,
                "Low priority lesson pregeneration"
        );
        generationJobPublisher.clear();

        generationJobWorker.processCourseGenerationJob(queuedJob.id());

        List<GenerationJob> lessonJobs = generationJobPublisher.publishedJobs().stream()
                .filter(job -> job.type() == GenerationJobType.LESSON_CONTENT)
                .map(PublishedJob::id)
                .map(jobId -> generationJobRepository.findById(jobId).orElseThrow())
                .toList();
        assertThat(lessonJobs).hasSize(2);
        assertThat(lessonJobs).extracting(GenerationJob::getPriority)
                .containsExactly(GenerationJobPriority.LOW, GenerationJobPriority.LOW);
        assertThat(lessonJobs).extracting(GenerationJob::getPrompt)
                .containsExactly("What is a Segment Tree?", "Building a Segment Tree");
    }

    @Test
    void openingLowPriorityPregeneratedLessonPromotesExistingJobToHighPriority() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|low-promotion-user", "low-promotion@example.com", "Low Promotion Student", null)
        );
        GenerationJobResponse queuedJob = generationJobService.createCourseGenerationJob(
                user,
                "Promote low priority lesson pregeneration"
        );
        generationJobWorker.processCourseGenerationJob(queuedJob.id());
        GenerationJobResponse completedCourseJob = generationJobService.getJobForUser(user, queuedJob.id());
        CourseResponse course = courseService.getCourseForUser(user, completedCourseJob.courseId());
        GenerationJob lowPriorityJob = generationJobRepository
                .findFirstByLessonIdAndTypeAndStatusInOrderByCreatedAtDesc(
                        course.modules().getFirst().lessons().getFirst().id(),
                        GenerationJobType.LESSON_CONTENT,
                        List.of(GenerationJobStatus.QUEUED, GenerationJobStatus.RUNNING)
                )
                .orElseThrow();
        generationJobPublisher.clear();

        generationJobService.createLessonGenerationJob(
                user,
                lowPriorityJob.getLessonId(),
                lowPriorityJob.getPrompt(),
                GenerationJobPriority.HIGH
        );

        GenerationJob promotedJob = generationJobRepository.findById(lowPriorityJob.getId()).orElseThrow();
        assertThat(promotedJob.getPriority()).isEqualTo(GenerationJobPriority.HIGH);
        assertThat(generationJobPublisher.publishedJobs())
                .contains(new PublishedJob(promotedJob.getId(), GenerationJobType.LESSON_CONTENT, GenerationJobPriority.HIGH));
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
    void createsLessonGenerationJobAndPublishesByPriority() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|lesson-job-user", "lesson-job@example.com", "Lesson Job Student", null)
        );
        CourseResponse course = courseService.createCourse(user, "Lesson job course");
        UUID lessonId = course.modules().getFirst().lessons().getFirst().id();
        generationJobPublisher.clear();

        GenerationJobResponse queuedJob = generationJobService.createLessonGenerationJob(
                user,
                lessonId,
                "What is a Segment Tree?",
                GenerationJobPriority.HIGH
        );

        assertThat(queuedJob.type()).isEqualTo(GenerationJobType.LESSON_CONTENT);
        assertThat(queuedJob.status()).isEqualTo(GenerationJobStatus.QUEUED);
        assertThat(queuedJob.priority()).isEqualTo(GenerationJobPriority.HIGH);
        assertThat(queuedJob.lessonId()).isEqualTo(lessonId);
        assertThat(generationJobPublisher.publishedJobs())
                .contains(new PublishedJob(queuedJob.id(), GenerationJobType.LESSON_CONTENT, GenerationJobPriority.HIGH));
    }

    @Test
    void reusesActiveLessonJobAndPromotesQueuedLowPriorityJob() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|lesson-promotion-user", "lesson-promotion@example.com", "Lesson Promotion Student", null)
        );
        CourseResponse course = courseService.createCourse(user, "Lesson promotion course");
        UUID lessonId = course.modules().getFirst().lessons().getFirst().id();
        GenerationJobResponse lowPriorityJob = generationJobService.createLessonGenerationJob(
                user,
                lessonId,
                "What is a Segment Tree?",
                GenerationJobPriority.LOW
        );
        generationJobPublisher.clear();

        GenerationJobResponse promotedJob = generationJobService.createLessonGenerationJob(
                user,
                lessonId,
                "What is a Segment Tree?",
                GenerationJobPriority.HIGH
        );

        assertThat(promotedJob.id()).isEqualTo(lowPriorityJob.id());
        assertThat(promotedJob.priority()).isEqualTo(GenerationJobPriority.HIGH);
        assertThat(generationJobPublisher.publishedJobs())
                .contains(new PublishedJob(promotedJob.id(), GenerationJobType.LESSON_CONTENT, GenerationJobPriority.HIGH));
        assertThat(generationJobRepository
                .findFirstByLessonIdAndTypeAndStatusInOrderByCreatedAtDesc(
                        lessonId,
                        GenerationJobType.LESSON_CONTENT,
                        List.of(GenerationJobStatus.QUEUED, GenerationJobStatus.RUNNING)
                ))
                .get()
                .extracting(GenerationJob::getPriority)
                .isEqualTo(GenerationJobPriority.HIGH);
    }

    @Test
    void lessonWorkerGeneratesLessonContentAndMarksJobSucceeded() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|lesson-worker-user", "lesson-worker@example.com", "Lesson Worker Student", null)
        );
        CourseResponse course = courseService.createCourse(user, "Lesson worker course");
        UUID lessonId = course.modules().getFirst().lessons().getFirst().id();
        GenerationJobResponse queuedJob = generationJobService.createLessonGenerationJob(
                user,
                lessonId,
                "What is a Segment Tree?",
                GenerationJobPriority.HIGH
        );

        generationJobWorker.processLessonGenerationJob(queuedJob.id());

        GenerationJobResponse completedJob = generationJobService.getJobForUser(user, queuedJob.id());
        assertThat(completedJob.status()).isEqualTo(GenerationJobStatus.SUCCEEDED);
        assertThat(completedJob.courseId()).isEqualTo(course.id());
        assertThat(completedJob.lessonId()).isEqualTo(lessonId);
        assertThat(completedJob.completedAt()).isNotNull();
        assertThat(completedJob.attemptCount()).isEqualTo(1);
        assertThat(courseService.getGeneratedLessonForUser(user, course.id(), 0, 0).content())
                .first()
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("text", "Generated lesson content.");
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

    @Test
    void republisherPublishesDueQueuedJobWhenInitialPublishWasMissed() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|missed-publish-user", "missed-publish@example.com", "Missed Publish Student", null)
        );
        GenerationJob job = generationJobRepository.saveAndFlush(
                new GenerationJob(user, GenerationJobType.COURSE_OUTLINE, "Missed publish course")
        );
        generationJobPublisher.clear();

        republisherScheduler().republishDueJobs();

        assertThat(generationJobPublisher.publishedJobIds()).contains(job.getId());
        GenerationJob republishedJob = generationJobRepository.findById(job.getId()).orElseThrow();
        assertThat(republishedJob.getLastPublishedAt()).isNotNull();
    }

    @Test
    void republisherSkipsRecentlyPublishedQueuedJobToAvoidSpam() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|recent-publish-user", "recent-publish@example.com", "Recent Publish Student", null)
        );
        GenerationJob job = generationJobRepository.saveAndFlush(
                new GenerationJob(user, GenerationJobType.COURSE_OUTLINE, "Recently published course")
        );
        generationJobTransitionService.markPublished(job.getId());
        generationJobPublisher.clear();

        republisherScheduler().republishDueJobs();

        assertThat(generationJobPublisher.publishedJobIds()).doesNotContain(job.getId());
    }

    @Test
    void recoveryRequeuesStaleRunningCourseJobWhenAttemptsRemain() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|stale-course-user", "stale-course@example.com", "Stale Course Student", null)
        );
        GenerationJob job = generationJobRepository.saveAndFlush(
                new GenerationJob(user, GenerationJobType.COURSE_OUTLINE, "Stale course job")
        );
        generationJobTransitionService.claim(job.getId(), "crashed-worker");
        makeRunningJobStale(job.getId(), OffsetDateTime.now().minusMinutes(21), 1);

        recoveryScheduler().recoverStaleRunningJobs();

        GenerationJob recoveredJob = generationJobRepository.findById(job.getId()).orElseThrow();
        assertThat(recoveredJob.getStatus()).isEqualTo(GenerationJobStatus.QUEUED);
        assertThat(recoveredJob.getLastErrorType()).isEqualTo(GenerationJobErrorType.UNKNOWN);
        assertThat(recoveredJob.getErrorMessage()).contains("COURSE_OUTLINE job recovered");
        assertThat(recoveredJob.getAttemptCount()).isEqualTo(1);
        assertThat(recoveredJob.getLockedBy()).isNull();
        assertThat(recoveredJob.getLockedAt()).isNull();
        assertThat(recoveredJob.getLastPublishedAt()).isNull();
    }

    @Test
    void recoveryFailsStaleRunningCourseJobWhenAttemptsAreExhausted() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|exhausted-course-user", "exhausted-course@example.com", "Exhausted Course Student", null)
        );
        GenerationJob job = generationJobRepository.saveAndFlush(
                new GenerationJob(user, GenerationJobType.COURSE_OUTLINE, "Exhausted stale course job")
        );
        generationJobTransitionService.claim(job.getId(), "crashed-worker");
        makeRunningJobStale(job.getId(), OffsetDateTime.now().minusMinutes(21), 4);

        recoveryScheduler().recoverStaleRunningJobs();

        GenerationJob failedJob = generationJobRepository.findById(job.getId()).orElseThrow();
        assertThat(failedJob.getStatus()).isEqualTo(GenerationJobStatus.FAILED);
        assertThat(failedJob.getLastErrorType()).isEqualTo(GenerationJobErrorType.UNKNOWN);
        assertThat(failedJob.getErrorMessage()).contains("COURSE_OUTLINE job recovered");
        assertThat(failedJob.getLockedBy()).isNull();
        assertThat(failedJob.getLockedAt()).isNull();
        assertThat(failedJob.getCompletedAt()).isNotNull();
    }

    @Test
    void recoveryUsesShorterTimeoutForLessonJobs() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|stale-lesson-user", "stale-lesson@example.com", "Stale Lesson Student", null)
        );
        GenerationJob job = generationJobRepository.saveAndFlush(new GenerationJob(
                user,
                GenerationJobType.LESSON_CONTENT,
                GenerationJobPriority.HIGH,
                "Stale lesson job",
                null
        ));
        generationJobTransitionService.claim(job.getId(), "crashed-worker");
        makeRunningJobStale(job.getId(), OffsetDateTime.now().minusMinutes(11), 1);

        recoveryScheduler().recoverStaleRunningJobs();

        GenerationJob recoveredJob = generationJobRepository.findById(job.getId()).orElseThrow();
        assertThat(recoveredJob.getStatus()).isEqualTo(GenerationJobStatus.QUEUED);
        assertThat(recoveredJob.getErrorMessage()).contains("LESSON_CONTENT job recovered");
    }

    private GenerationJobRepublisherScheduler republisherScheduler() {
        return new GenerationJobRepublisherScheduler(generationJobRepository, generationJobPublisher);
    }

    private GenerationJobRecoveryScheduler recoveryScheduler() {
        return new GenerationJobRecoveryScheduler(generationJobRepository, generationJobTransitionService);
    }

    private void makeRunningJobStale(UUID jobId, OffsetDateTime lockedAt, int attemptCount) {
        jdbcTemplate.update(
                "update generation_jobs set locked_at = ?, attempt_count = ?, updated_at = ? where id = ?",
                lockedAt,
                attemptCount,
                lockedAt,
                jobId
        );
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
                            List.of(
                                    "What is a Segment Tree?",
                                    "Building a Segment Tree",
                                    "Range Sum Queries"
                            )
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

    record PublishedJob(UUID id, GenerationJobType type, GenerationJobPriority priority) {
    }

    static class CapturingGenerationJobPublisher implements GenerationJobPublisher {

        private final GenerationJobTransitionService generationJobTransitionService;
        private final List<PublishedJob> publishedJobs = new CopyOnWriteArrayList<>();

        CapturingGenerationJobPublisher(GenerationJobTransitionService generationJobTransitionService) {
            this.generationJobTransitionService = generationJobTransitionService;
        }

        @Override
        public void publishGenerationJob(UUID jobId, GenerationJobType type, GenerationJobPriority priority) {
            publishedJobs.add(new PublishedJob(jobId, type, priority));
            generationJobTransitionService.markPublished(jobId);
        }

        @Override
        public void publishCourseGenerationJob(UUID jobId) {
            publishGenerationJob(jobId, GenerationJobType.COURSE_OUTLINE, GenerationJobPriority.NORMAL);
        }

        void clear() {
            publishedJobs.clear();
        }

        List<UUID> publishedJobIds() {
            return publishedJobs.stream().map(PublishedJob::id).toList();
        }

        List<PublishedJob> publishedJobs() {
            return publishedJobs;
        }
    }
}
