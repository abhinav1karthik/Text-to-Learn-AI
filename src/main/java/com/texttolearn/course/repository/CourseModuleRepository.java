package com.texttolearn.course.repository;

import com.texttolearn.course.model.CourseModule;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseModuleRepository extends JpaRepository<CourseModule, UUID> {
}
