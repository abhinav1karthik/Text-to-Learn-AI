package com.texttolearn.user.controller;

import com.texttolearn.security.Auth0UserInfoService;
import com.texttolearn.security.AuthenticatedUserProfile;
import com.texttolearn.user.model.AppUser;
import com.texttolearn.user.service.AppUserService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class CurrentUserController {

    private final AppUserService appUserService;
    private final Auth0UserInfoService auth0UserInfoService;

    public CurrentUserController(AppUserService appUserService, Auth0UserInfoService auth0UserInfoService) {
        this.appUserService = appUserService;
        this.auth0UserInfoService = auth0UserInfoService;
    }

    @GetMapping
    CurrentUserResponse getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUserProfile profile = auth0UserInfoService.getUserProfile(jwt);
        AppUser appUser = appUserService.syncFromProfile(profile);
        return new CurrentUserResponse(
                appUser.getId(),
                appUser.getAuth0Subject(),
                appUser.getEmail(),
                appUser.getName(),
                appUser.getPictureUrl()
        );
    }

    record CurrentUserResponse(
            UUID id,
            String auth0Subject,
            String email,
            String name,
            String pictureUrl
    ) {
    }
}
