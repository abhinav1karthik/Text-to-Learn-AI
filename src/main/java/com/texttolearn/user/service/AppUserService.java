package com.texttolearn.user.service;

import com.texttolearn.user.model.AppUser;
import com.texttolearn.user.repository.AppUserRepository;
import com.texttolearn.security.AuthenticatedUserProfile;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;

    public AppUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> findByAuth0Subject(String auth0Subject) {
        return appUserRepository.findByAuth0Subject(auth0Subject);
    }

    @Transactional
    public AppUser syncFromProfile(AuthenticatedUserProfile profile) {
        AppUser appUser = appUserRepository.findByAuth0Subject(profile.auth0Subject())
                .orElseGet(() -> new AppUser(
                        profile.auth0Subject(),
                        profile.email(),
                        profile.name(),
                        profile.pictureUrl()
                ));
        appUser.updateProfile(profile.email(), profile.name(), profile.pictureUrl());
        return appUserRepository.save(appUser);
    }
}
