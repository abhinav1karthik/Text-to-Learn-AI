package com.texttolearn.course.repository;

import com.texttolearn.course.model.Lesson;
import com.texttolearn.user.model.AppUser;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    @Query("""
            select lesson
            from Lesson lesson
            join fetch lesson.module module
            join fetch module.course course
            where lesson.id = :lessonId
              and course.user = :user
            """)
    Optional<Lesson> findByIdAndCourseUser(
            @Param("lessonId") UUID lessonId,
            @Param("user") AppUser user
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select lesson
            from Lesson lesson
            join fetch lesson.module module
            join fetch module.course course
            where lesson.id = :lessonId
              and course.user = :user
            """)
    Optional<Lesson> findByIdAndCourseUserForUpdate(
            @Param("lessonId") UUID lessonId,
            @Param("user") AppUser user
    );
}
