package com.texttolearn.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.texttolearn.ai.dto.GeneratedCourseOutline;
import com.texttolearn.ai.dto.GeneratedLessonContent;
import com.texttolearn.ai.dto.GeneratedModuleOutline;
import com.texttolearn.ai.service.CourseAiService;
import com.texttolearn.course.dto.CourseResponse;
import com.texttolearn.course.dto.LessonResponse;
import com.texttolearn.course.model.Course;
import com.texttolearn.course.model.CourseStatus;
import com.texttolearn.course.model.Lesson;
import com.texttolearn.course.model.LessonStatus;
import com.texttolearn.course.repository.CourseRepository;
import com.texttolearn.course.service.CourseService;
import com.texttolearn.user.model.AppUser;
import com.texttolearn.user.repository.AppUserRepository;
import com.texttolearn.video.config.YouTubeProperties;
import com.texttolearn.video.dto.YouTubeVideoResponse;
import com.texttolearn.video.dto.YouTubeVideoSearchResponse;
import com.texttolearn.video.service.YouTubeVideoService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class CourseServiceTests {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    @Transactional
    void createsCourseFromGeneratedOutline() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|course-service-user", "student@example.com", "Student", null)
        );

        CourseResponse course = courseService.createCourse(user, "Segment Trees and Its Applications");

        assertThat(course.id()).isNotNull();
        assertThat(course.prompt()).isEqualTo("Segment Trees and Its Applications");
        assertThat(course.title()).isEqualTo("Segment Trees for Range Queries");
        assertThat(course.status()).isEqualTo(CourseStatus.OUTLINE_READY);
        assertThat(course.tags()).containsExactly("data-structures", "algorithms");
        assertThat(course.modules()).hasSize(1);
        assertThat(course.modules().getFirst().lessons()).hasSize(2);
        assertThat(course.modules().getFirst().lessons().getFirst().status()).isEqualTo(LessonStatus.PLANNED);
    }

    @Test
    @Transactional
    void generatesLessonContentLazilyAndCachesIt() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|lesson-service-user", "lesson@example.com", "Lesson Student", null)
        );
        CourseResponse course = courseService.createCourse(user, "Segment Trees and Its Applications");

        LessonResponse generatedLesson = courseService.getOrGenerateLessonForUser(user, course.id(), 0, 0);
        LessonResponse cachedLesson = courseService.getOrGenerateLessonForUser(user, course.id(), 0, 0);

        assertThat(generatedLesson.title()).isEqualTo("What is a Segment Tree?");
        assertThat(generatedLesson.status()).isEqualTo(LessonStatus.GENERATED);
        assertThat(generatedLesson.objectives()).hasSize(1);
        assertThat(generatedLesson.objectives().getFirst()).startsWith("Understand call ");
        assertThat(generatedLesson.content()).hasSize(2);
        assertThat(generatedLesson.content().getFirst()).containsEntry("type", "heading");
        assertThat(cachedLesson.objectives()).isEqualTo(generatedLesson.objectives());
    }

    @Test
    @Transactional
    void returnsAlreadyGeneratedLessonWhenOldSavedCodeBlockIsEmpty() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|old-code-user", "old-code@example.com", "Old Code Student", null)
        );
        CourseResponse courseResponse = courseService.createCourse(user, "Segment Trees and Its Applications");
        Course course = courseRepository.findById(courseResponse.id()).orElseThrow();
        Lesson lesson = course.getModules().getFirst().getLessons().getFirst();
        lesson.replaceGeneratedContent(
                "[\"Use the saved lesson\"]",
                "[{\"type\":\"code\",\"language\":\"java\",\"text\":\"\"}]"
        );
        courseRepository.saveAndFlush(course);

        LessonResponse response = courseService.getOrGenerateLessonForUser(user, courseResponse.id(), 0, 0);

        assertThat(response.status()).isEqualTo(LessonStatus.GENERATED);
        assertThat(response.objectives()).containsExactly("Use the saved lesson");
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst()).containsEntry("type", "paragraph");
        assertThat(response.content().getFirst()).containsEntry(
                "text",
                "Code example was not available for this section."
        );
    }

    @Test
    @Transactional
    void keepsMcqsAtEndAndDistributesVideoBlocksThroughLessonContent() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|video-flow-user", "video-flow@example.com", "Video Flow Student", null)
        );
        CourseResponse course = courseService.createCourse(user, "Segment Trees and Its Applications");

        LessonResponse response = courseService.getOrGenerateLessonForUser(user, course.id(), 0, 1);

        assertThat(response.content()).extracting(block -> block.get("type"))
                .containsExactly("heading", "paragraph", "video", "paragraph", "video", "mcq", "mcq");
        assertThat(response.content().get(2)).containsEntry("maxResults", 1);
        assertThat(response.content().get(4)).containsEntry("maxResults", 1);
        assertThat(response.content().get(2).get("videos")).asList().hasSize(1);
        assertThat(response.content().get(2).get("videos").toString())
                .contains("https://www.youtube.com/watch?v=video-1");
    }

    @TestConfiguration
    static class FakeAiConfig {

        @Bean
        @Primary
        CourseAiService fakeCourseAiService() {
            AtomicInteger lessonGenerationCount = new AtomicInteger();
            return new CourseAiService() {
                @Override
                public GeneratedCourseOutline generateCourseOutline(String topic) {
                    return new GeneratedCourseOutline(
                            "Segment Trees for Range Queries",
                            "A focused course on segment trees.",
                            List.of("Data-Structures", "Algorithms"),
                            List.of(new GeneratedModuleOutline(
                                    "Foundations",
                                    "Core ideas behind segment trees.",
                                    List.of("What is a Segment Tree?", "Building the Tree")
                            ))
                    );
                }

                @Override
                public GeneratedLessonContent generateLessonContent(
                        String courseTitle,
                        String moduleTitle,
                        String lessonTitle
                ) {
                    int callNumber = lessonGenerationCount.incrementAndGet();
                    if ("Building the Tree".equals(lessonTitle)) {
                        return new GeneratedLessonContent(
                                lessonTitle,
                                List.of("Understand video placement"),
                                List.of(
                                        Map.of("type", "heading", "text", lessonTitle),
                                        Map.of("type", "paragraph", "text", "First explanation."),
                                        Map.of("type", "paragraph", "text", "Second explanation."),
                                        Map.of(
                                                "type", "video",
                                                "title", "First related video",
                                                "query", "segment tree build visual explanation"
                                        ),
                                        Map.of(
                                                "type", "video",
                                                "title", "Second related video",
                                                "query", "segment tree implementation walkthrough"
                                        ),
                                        Map.of(
                                                "type", "mcq",
                                                "question", "Question one?",
                                                "options", List.of("A", "B", "C", "D"),
                                                "answer", 1,
                                                "explanation", "B is correct."
                                        ),
                                        Map.of(
                                                "type", "mcq",
                                                "question", "Question two?",
                                                "options", List.of("A", "B", "C", "D"),
                                                "answer", 2,
                                                "explanation", "C is correct."
                                        )
                                )
                        );
                    }

                    return new GeneratedLessonContent(
                            lessonTitle,
                            List.of("Understand call " + callNumber),
                            List.of(
                                    Map.of("type", "heading", "text", lessonTitle),
                                    Map.of(
                                            "type", "paragraph",
                                            "text", courseTitle + " / " + moduleTitle
                                    )
                            )
                    );
                }
            };
        }

        @Bean
        @Primary
        YouTubeVideoService fakeYouTubeVideoService(ObjectMapper objectMapper) {
            return new YouTubeVideoService(
                    objectMapper,
                    new YouTubeProperties("test-key", "https://youtube.googleapis.com/youtube/v3", 1, 60)
            ) {
                private final AtomicInteger videoCount = new AtomicInteger();

                @Override
                public YouTubeVideoSearchResponse searchEducationalVideos(String query, Integer maxResults) {
                    int videoNumber = videoCount.incrementAndGet();
                    String videoId = "video-" + videoNumber;
                    return new YouTubeVideoSearchResponse(
                            query,
                            List.of(new YouTubeVideoResponse(
                                    videoId,
                                    "https://www.youtube.com/embed/" + videoId,
                                    "https://www.youtube.com/watch?v=" + videoId,
                                    "Saved video for " + query,
                                    "Text To Learn Test Channel",
                                    "https://img.youtube.com/" + videoId + ".jpg"
                            ))
                    );
                }
            };
        }
    }
}
