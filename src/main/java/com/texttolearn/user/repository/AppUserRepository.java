package com.texttolearn.user.repository;

import com.texttolearn.user.model.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByAuth0Subject(String auth0Subject);
}
