package com.texttolearn.generation.repository;

import com.texttolearn.generation.model.GenerationJob;
import com.texttolearn.generation.model.GenerationJobStatus;
import com.texttolearn.generation.model.GenerationJobType;
import com.texttolearn.user.model.AppUser;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GenerationJobRepository extends JpaRepository<GenerationJob, UUID> {

    Optional<GenerationJob> findByIdAndUser(UUID id, AppUser user);

    Optional<GenerationJob> findFirstByUserAndTypeAndStatusInOrderByCreatedAtDesc(
            AppUser user,
            GenerationJobType type,
            Collection<GenerationJobStatus> statuses
    );

    Optional<GenerationJob> findFirstByLessonIdAndTypeAndStatusInOrderByCreatedAtDesc(
            UUID lessonId,
            GenerationJobType type,
            Collection<GenerationJobStatus> statuses
    );

    @Query("select job from GenerationJob job join fetch job.user where job.id = :id")
    Optional<GenerationJob> findByIdWithUser(@Param("id") UUID id);

    @Query("""
            select job
            from GenerationJob job
            join fetch job.user
            where job.id = :id
              and job.lockedBy = :lockedBy
            """)
    Optional<GenerationJob> findByIdWithUserAndLockedBy(
            @Param("id") UUID id,
            @Param("lockedBy") String lockedBy
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update generation_jobs
            set status = 'RUNNING',
                attempt_count = attempt_count + 1,
                locked_at = current_timestamp,
                locked_by = :lockedBy,
                started_at = current_timestamp,
                completed_at = null,
                updated_at = current_timestamp,
                error_message = null,
                last_error_type = null
            where id = :id
              and status = 'QUEUED'
              and next_run_at <= current_timestamp
              and attempt_count < max_attempts
            """, nativeQuery = true)
    int claimQueuedJob(
            @Param("id") UUID id,
            @Param("lockedBy") String lockedBy
    );
}
