package com.texttolearn.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.texttolearn.ai.dto.GeneratedCourseOutline;
import com.texttolearn.ai.dto.GeneratedLessonContent;
import com.texttolearn.ai.dto.GeneratedModuleOutline;
import com.texttolearn.ai.service.CourseAiService;
import com.texttolearn.course.repository.CourseRepository;
import com.texttolearn.generation.dto.GenerationJobResponse;
import com.texttolearn.generation.model.GenerationJob;
import com.texttolearn.generation.model.GenerationJobErrorType;
import com.texttolearn.generation.model.GenerationJobStatus;
import com.texttolearn.generation.model.GenerationJobType;
import com.texttolearn.generation.repository.GenerationJobRepository;
import com.texttolearn.generation.service.GenerationJobService;
import com.texttolearn.generation.service.GenerationJobTransitionService;
import com.texttolearn.user.model.AppUser;
import com.texttolearn.user.repository.AppUserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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

    @Test
    void createsCourseGenerationJobAndCompletesItInBackground() throws InterruptedException {
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

        GenerationJobResponse completedJob = waitForTerminalJob(user, queuedJob);

        assertThat(completedJob.status()).isEqualTo(GenerationJobStatus.SUCCEEDED);
        assertThat(completedJob.courseId()).isNotNull();
        assertThat(completedJob.completedAt()).isNotNull();
        assertThat(courseRepository.findByIdAndUser(completedJob.courseId(), user)).isPresent();
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

    private GenerationJobResponse waitForTerminalJob(
            AppUser user,
            GenerationJobResponse initialJob
    ) throws InterruptedException {
        GenerationJobResponse currentJob = initialJob;
        for (int attempt = 0; attempt < 30; attempt++) {
            currentJob = generationJobService.getJobForUser(user, initialJob.id());
            if (currentJob.status() == GenerationJobStatus.SUCCEEDED
                    || currentJob.status() == GenerationJobStatus.FAILED) {
                return currentJob;
            }
            Thread.sleep(100);
        }

        return currentJob;
    }

    @TestConfiguration
    static class FakeAiConfig {

        @Bean
        @Primary
        CourseAiService fakeCourseAiService() {
            return new CourseAiService() {
                @Override
                public GeneratedCourseOutline generateCourseOutline(String topic) {
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
            };
        }
    }
}
