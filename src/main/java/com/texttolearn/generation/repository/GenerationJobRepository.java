package com.texttolearn.generation.repository;

import com.texttolearn.generation.model.GenerationJob;
import com.texttolearn.user.model.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GenerationJobRepository extends JpaRepository<GenerationJob, UUID> {

    Optional<GenerationJob> findByIdAndUser(UUID id, AppUser user);

    @Query("select job from GenerationJob job join fetch job.user where job.id = :id")
    Optional<GenerationJob> findByIdWithUser(@Param("id") UUID id);
}
