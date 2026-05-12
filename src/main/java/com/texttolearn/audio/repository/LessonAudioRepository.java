package com.texttolearn.audio.repository;

import com.texttolearn.audio.model.LessonAudio;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonAudioRepository extends JpaRepository<LessonAudio, UUID> {

    Optional<LessonAudio> findByLessonIdAndLanguageAndVoiceName(UUID lessonId, String language, String voiceName);
}
