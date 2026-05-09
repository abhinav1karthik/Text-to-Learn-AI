package com.texttolearn.course.service;

import com.texttolearn.course.model.Course;
import com.texttolearn.course.repository.CourseRepository;
import com.texttolearn.user.model.AppUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public List<Course> findCoursesForUser(AppUser user) {
        return courseRepository.findByUserOrderByCreatedAtDesc(user);
    }
}
