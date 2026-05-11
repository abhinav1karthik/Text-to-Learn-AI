package com.texttolearn.ai.service;

import com.texttolearn.ai.dto.GeneratedCourseOutline;
import com.texttolearn.ai.dto.GeneratedLessonContent;

public interface CourseAiService {

    GeneratedCourseOutline generateCourseOutline(String topic);

    GeneratedLessonContent generateLessonContent(String courseTitle, String moduleTitle, String lessonTitle);
}
