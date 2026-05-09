package com.texttolearn.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.texttolearn.course.model.Course;
import com.texttolearn.course.model.CourseModule;
import com.texttolearn.course.model.CourseStatus;
import com.texttolearn.course.model.Lesson;
import com.texttolearn.course.model.LessonStatus;
import com.texttolearn.course.repository.CourseRepository;
import com.texttolearn.user.model.AppUser;
import com.texttolearn.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class CourseRepositoryTests {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    @Transactional
    void savesCourseWithModulesAndLessons() {
        AppUser user = appUserRepository.save(
                new AppUser("auth0|user-123", "student@example.com", "Student", null)
        );

        Course course = new Course(
                user,
                "Segment Trees and Its Applications",
                "Segment Trees and Applications",
                "A practical course for learning segment trees."
        );

        CourseModule module = new CourseModule("Foundations", "Core segment tree concepts.", 1);
        module.addLesson(new Lesson("What is a Segment Tree?", 1));
        module.addLesson(new Lesson("Build Operation", 2));
        course.addModule(module);

        Course saved = courseRepository.saveAndFlush(course);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(CourseStatus.OUTLINE_READY);
        assertThat(saved.getTags()).isEqualTo("[]");
        assertThat(saved.getModules()).hasSize(1);
        assertThat(saved.getModules().getFirst().getCreatedAt()).isNotNull();
        assertThat(saved.getModules().getFirst().getLessons()).hasSize(2);
        assertThat(saved.getModules().getFirst().getLessons().getFirst().getStatus()).isEqualTo(LessonStatus.PLANNED);
        assertThat(saved.getModules().getFirst().getLessons().getFirst().getContentJson()).isEqualTo("[]");
        assertThat(saved.getModules().getFirst().getLessons().getFirst().isEnriched()).isFalse();
    }
}
