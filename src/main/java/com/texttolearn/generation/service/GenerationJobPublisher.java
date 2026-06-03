package com.texttolearn.generation.service;

import java.util.UUID;

public interface GenerationJobPublisher {

    void publishCourseGenerationJob(UUID jobId);
}
