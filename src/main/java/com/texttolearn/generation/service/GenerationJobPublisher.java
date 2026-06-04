package com.texttolearn.generation.service;

import com.texttolearn.generation.model.GenerationJobPriority;
import com.texttolearn.generation.model.GenerationJobType;
import java.util.UUID;

public interface GenerationJobPublisher {

    void publishGenerationJob(UUID jobId, GenerationJobType type, GenerationJobPriority priority);

    void publishCourseGenerationJob(UUID jobId);
}
