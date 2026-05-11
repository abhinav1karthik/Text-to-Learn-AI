package com.texttolearn.course.controller;

import com.texttolearn.course.dto.CourseResponse;
import com.texttolearn.course.dto.CreateCourseRequest;
import com.texttolearn.course.dto.LessonResponse;
import com.texttolearn.course.service.CourseService;
import com.texttolearn.security.Auth0UserInfoService;
import com.texttolearn.security.AuthenticatedUserProfile;
import com.texttolearn.user.model.AppUser;
import com.texttolearn.user.service.AppUserService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final AppUserService appUserService;
    private final Auth0UserInfoService auth0UserInfoService;
    private final CourseService courseService;

    public CourseController(
            AppUserService appUserService,
            Auth0UserInfoService auth0UserInfoService,
            CourseService courseService
    ) {
        this.appUserService = appUserService;
        this.auth0UserInfoService = auth0UserInfoService;
        this.courseService = courseService;
    }

    @PostMapping
    ResponseEntity<CourseResponse> createCourse(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCourseRequest request
    ) {
        AppUser user = currentUser(jwt);
        CourseResponse response = courseService.createCourse(user, request.topic());
        return ResponseEntity
                .created(URI.create("/api/courses/" + response.id()))
                .body(response);
    }

    @GetMapping
    List<CourseResponse> getCourses(@AuthenticationPrincipal Jwt jwt) {
        AppUser user = currentUser(jwt);
        return courseService.getCoursesForUser(user);
    }

    @GetMapping("/{courseId}")
    CourseResponse getCourse(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId) {
        AppUser user = currentUser(jwt);
        return courseService.getCourseForUser(user, courseId);
    }

    @GetMapping("/{courseId}/module/{moduleIndex}/lesson/{lessonIndex}")
    LessonResponse getLesson(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID courseId,
            @PathVariable int moduleIndex,
            @PathVariable int lessonIndex
    ) {
        AppUser user = currentUser(jwt);
        return courseService.getOrGenerateLessonForUser(user, courseId, moduleIndex, lessonIndex);
    }

    private AppUser currentUser(Jwt jwt) {
        AuthenticatedUserProfile profile = auth0UserInfoService.getUserProfile(jwt);
        return appUserService.syncFromProfile(profile);
    }
}
