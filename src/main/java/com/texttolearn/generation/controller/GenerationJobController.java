package com.texttolearn.generation.controller;

import com.texttolearn.generation.dto.CreateCourseGenerationJobRequest;
import com.texttolearn.generation.dto.GenerationJobResponse;
import com.texttolearn.generation.service.GenerationJobService;
import com.texttolearn.security.Auth0UserInfoService;
import com.texttolearn.security.AuthenticatedUserProfile;
import com.texttolearn.user.model.AppUser;
import com.texttolearn.user.service.AppUserService;
import jakarta.validation.Valid;
import java.net.URI;
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
@RequestMapping("/api/generation-jobs")
public class GenerationJobController {

    private final AppUserService appUserService;
    private final Auth0UserInfoService auth0UserInfoService;
    private final GenerationJobService generationJobService;

    public GenerationJobController(
            AppUserService appUserService,
            Auth0UserInfoService auth0UserInfoService,
            GenerationJobService generationJobService
    ) {
        this.appUserService = appUserService;
        this.auth0UserInfoService = auth0UserInfoService;
        this.generationJobService = generationJobService;
    }

    @PostMapping("/course")
    ResponseEntity<GenerationJobResponse> createCourseGenerationJob(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCourseGenerationJobRequest request
    ) {
        AppUser user = currentUser(jwt);
        GenerationJobResponse response = generationJobService.createCourseGenerationJob(user, request.topic());
        return ResponseEntity
                .created(URI.create("/api/generation-jobs/" + response.id()))
                .body(response);
    }

    @GetMapping("/{jobId}")
    GenerationJobResponse getGenerationJob(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID jobId
    ) {
        AppUser user = currentUser(jwt);
        return generationJobService.getJobForUser(user, jobId);
    }

    private AppUser currentUser(Jwt jwt) {
        AuthenticatedUserProfile profile = auth0UserInfoService.getUserProfile(jwt);
        return appUserService.syncFromProfile(profile);
    }
}
