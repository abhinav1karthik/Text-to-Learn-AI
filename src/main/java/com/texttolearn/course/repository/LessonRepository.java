package com.texttolearn.course.repository;

import com.texttolearn.course.model.Lesson;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {
}
