package com.texttolearn.course.controller;

import com.texttolearn.audio.dto.LessonAudioResponse;
import com.texttolearn.audio.service.LessonAudioService;
import com.texttolearn.course.dto.CourseResponse;
import com.texttolearn.course.dto.CreateCourseRequest;
import com.texttolearn.course.dto.LessonResponse;
import com.texttolearn.course.service.CourseService;
import com.texttolearn.pdf.dto.LessonPdfResponse;
import com.texttolearn.pdf.service.LessonPdfService;
import com.texttolearn.security.Auth0UserInfoService;
import com.texttolearn.security.AuthenticatedUserProfile;
import com.texttolearn.user.model.AppUser;
import com.texttolearn.user.service.AppUserService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final AppUserService appUserService;
    private final Auth0UserInfoService auth0UserInfoService;
    private final CourseService courseService;
    private final LessonAudioService lessonAudioService;
    private final LessonPdfService lessonPdfService;

    public CourseController(
            AppUserService appUserService,
            Auth0UserInfoService auth0UserInfoService,
            CourseService courseService,
            LessonAudioService lessonAudioService,
            LessonPdfService lessonPdfService
    ) {
        this.appUserService = appUserService;
        this.auth0UserInfoService = auth0UserInfoService;
        this.courseService = courseService;
        this.lessonAudioService = lessonAudioService;
        this.lessonPdfService = lessonPdfService;
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

    @GetMapping(value = "/{courseId}/module/{moduleIndex}/lesson/{lessonIndex}/audio")
    ResponseEntity<byte[]> getLessonAudio(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID courseId,
            @PathVariable int moduleIndex,
            @PathVariable int lessonIndex,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String voiceName
    ) {
        AppUser user = currentUser(jwt);
        LessonResponse lesson = courseService.getGeneratedLessonForUser(user, courseId, moduleIndex, lessonIndex);
        LessonAudioResponse audio = lessonAudioService.generateAudio(lesson, voiceName, language);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(audio.contentType()))
                .contentLength(audio.audio().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + audio.fileName() + "\"")
                .body(audio.audio());
    }

    @GetMapping(value = "/{courseId}/module/{moduleIndex}/lesson/{lessonIndex}/pdf")
    ResponseEntity<byte[]> getLessonPdf(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID courseId,
            @PathVariable int moduleIndex,
            @PathVariable int lessonIndex
    ) {
        AppUser user = currentUser(jwt);
        LessonResponse lesson = courseService.getGeneratedLessonForUser(user, courseId, moduleIndex, lessonIndex);
        LessonPdfResponse pdf = lessonPdfService.generateLessonPdf(lesson);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(pdf.contentType()))
                .contentLength(pdf.pdf().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pdf.fileName() + "\"")
                .body(pdf.pdf());
    }

    private AppUser currentUser(Jwt jwt) {
        AuthenticatedUserProfile profile = auth0UserInfoService.getUserProfile(jwt);
        return appUserService.syncFromProfile(profile);
    }
}
