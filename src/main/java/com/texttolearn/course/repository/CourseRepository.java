package com.texttolearn.course.repository;

import com.texttolearn.course.model.Course;
import com.texttolearn.user.model.AppUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    List<Course> findByUserOrderByCreatedAtDesc(AppUser user);

    Optional<Course> findByIdAndUser(UUID id, AppUser user);
}
