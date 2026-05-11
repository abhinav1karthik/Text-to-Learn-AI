package com.texttolearn.course.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.texttolearn.ai.dto.GeneratedCourseOutline;
import com.texttolearn.ai.dto.GeneratedLessonContent;
import com.texttolearn.ai.dto.GeneratedModuleOutline;
import com.texttolearn.ai.error.AiGenerationException;
import com.texttolearn.ai.service.CourseAiService;
import com.texttolearn.common.error.ResourceNotFoundException;
import com.texttolearn.course.dto.CourseResponse;
import com.texttolearn.course.dto.LessonResponse;
import com.texttolearn.course.dto.LessonSummaryResponse;
import com.texttolearn.course.dto.ModuleResponse;
import com.texttolearn.course.model.Course;
import com.texttolearn.course.model.CourseModule;
import com.texttolearn.course.model.Lesson;
import com.texttolearn.course.model.LessonStatus;
import com.texttolearn.course.repository.CourseRepository;
import com.texttolearn.user.model.AppUser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> CONTENT_BLOCK_LIST_TYPE = new TypeReference<>() {
    };

    private final CourseAiService courseAiService;
    private final ObjectMapper objectMapper;
    private final CourseRepository courseRepository;

    public CourseService(CourseAiService courseAiService, ObjectMapper objectMapper, CourseRepository courseRepository) {
        this.courseAiService = courseAiService;
        this.objectMapper = objectMapper;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public List<Course> findCoursesForUser(AppUser user) {
        return courseRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesForUser(AppUser user) {
        return findCoursesForUser(user).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CourseResponse createCourse(AppUser user, String topic) {
        GeneratedCourseOutline outline = courseAiService.generateCourseOutline(topic);
        validateOutline(outline);

        Course course = new Course(
                user,
                topic.trim(),
                outline.title().trim(),
                outline.description()
        );
        course.replaceTagsJson(writeTagsJson(outline.tags()));

        for (int moduleIndex = 0; moduleIndex < outline.modules().size(); moduleIndex++) {
            GeneratedModuleOutline moduleOutline = outline.modules().get(moduleIndex);
            CourseModule module = new CourseModule(
                    moduleOutline.title().trim(),
                    moduleOutline.summary(),
                    moduleIndex + 1
            );

            List<String> lessonTitles = moduleOutline.lessons();
            for (int lessonIndex = 0; lessonIndex < lessonTitles.size(); lessonIndex++) {
                module.addLesson(new Lesson(lessonTitles.get(lessonIndex).trim(), lessonIndex + 1));
            }

            course.addModule(module);
        }

        Course savedCourse = courseRepository.saveAndFlush(course);
        return toResponse(savedCourse);
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseForUser(AppUser user, UUID courseId) {
        Course course = courseRepository.findByIdAndUser(courseId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        return toResponse(course);
    }

    @Transactional
    public LessonResponse getOrGenerateLessonForUser(AppUser user, UUID courseId, int moduleIndex, int lessonIndex) {
        if (moduleIndex < 0 || lessonIndex < 0) {
            throw new ResourceNotFoundException("Lesson not found");
        }

        Course course = courseRepository.findByIdAndUser(courseId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        CourseModule module = findModuleByIndex(course, moduleIndex);
        Lesson lesson = findLessonByIndex(module, lessonIndex);

        if (needsGeneration(lesson)) {
            GeneratedLessonContent generatedLesson = courseAiService.generateLessonContent(
                    course.getTitle(),
                    module.getTitle(),
                    lesson.getTitle()
            );
            validateLessonContent(generatedLesson);
            lesson.replaceGeneratedContent(
                    writeObjectivesJson(generatedLesson.objectives()),
                    writeContentJson(generatedLesson.content())
            );
        }

        return toLessonResponse(course, module, lesson);
    }

    public CourseResponse toResponse(Course course) {
        List<ModuleResponse> modules = course.getModules().stream()
                .map(module -> new ModuleResponse(
                        module.getId(),
                        module.getTitle(),
                        module.getSummary(),
                        module.getPosition(),
                        module.getLessons().stream()
                                .map(lesson -> new LessonSummaryResponse(
                                        lesson.getId(),
                                        lesson.getTitle(),
                                        lesson.getPosition(),
                                        lesson.getStatus()
                                ))
                                .toList()
                ))
                .toList();

        return new CourseResponse(
                course.getId(),
                course.getPrompt(),
                course.getTitle(),
                course.getDescription(),
                readTags(course.getTags()),
                course.getStatus(),
                course.getCreatedAt(),
                course.getUpdatedAt(),
                modules
        );
    }

    private LessonResponse toLessonResponse(Course course, CourseModule module, Lesson lesson) {
        return new LessonResponse(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getPosition(),
                lesson.getStatus(),
                readObjectives(lesson.getObjectivesJson()),
                readContent(lesson.getContentJson()),
                module.getId(),
                module.getTitle(),
                course.getId(),
                course.getTitle()
        );
    }

    private CourseModule findModuleByIndex(Course course, int moduleIndex) {
        int position = moduleIndex + 1;
        return course.getModules().stream()
                .filter(module -> module.getPosition() == position)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Module not found"));
    }

    private Lesson findLessonByIndex(CourseModule module, int lessonIndex) {
        int position = lessonIndex + 1;
        return module.getLessons().stream()
                .filter(lesson -> lesson.getPosition() == position)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
    }

    private boolean needsGeneration(Lesson lesson) {
        return lesson.getStatus() == LessonStatus.PLANNED
                || lesson.getContentJson() == null
                || lesson.getContentJson().isBlank()
                || "[]".equals(lesson.getContentJson().trim());
    }

    private void validateOutline(GeneratedCourseOutline outline) {
        if (outline == null) {
            throw new AiGenerationException("AI did not return a course outline.");
        }

        if (isBlank(outline.title())) {
            throw new AiGenerationException("AI course outline is missing a title.");
        }

        if (outline.modules() == null || outline.modules().isEmpty()) {
            throw new AiGenerationException("AI course outline is missing modules.");
        }

        for (GeneratedModuleOutline module : outline.modules()) {
            if (module == null || isBlank(module.title())) {
                throw new AiGenerationException("AI course outline contains a module without a title.");
            }

            if (module.lessons() == null || module.lessons().isEmpty()) {
                throw new AiGenerationException("AI course outline contains a module without lessons.");
            }

            boolean hasBlankLesson = module.lessons().stream().anyMatch(this::isBlank);
            if (hasBlankLesson) {
                throw new AiGenerationException("AI course outline contains a blank lesson title.");
            }
        }
    }

    private void validateLessonContent(GeneratedLessonContent lessonContent) {
        if (lessonContent == null) {
            throw new AiGenerationException("AI did not return lesson content.");
        }

        if (lessonContent.objectives() == null || lessonContent.objectives().isEmpty()) {
            throw new AiGenerationException("AI lesson content is missing objectives.");
        }

        boolean hasBlankObjective = lessonContent.objectives().stream().anyMatch(this::isBlank);
        if (hasBlankObjective) {
            throw new AiGenerationException("AI lesson content contains a blank objective.");
        }

        List<Map<String, Object>> normalizedContent = normalizeContentBlocks(lessonContent.content());
        if (normalizedContent.isEmpty()) {
            throw new AiGenerationException("AI lesson content is missing content blocks.");
        }

        for (Map<String, Object> block : normalizedContent) {
            Object blockType = block == null ? null : block.get("type");
            if (blockType == null || isBlank(String.valueOf(blockType))) {
                throw new AiGenerationException("AI lesson content contains a block without a type.");
            }
        }
    }

    private String writeTagsJson(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(normalizeTags(tags));
        } catch (JsonProcessingException exception) {
            throw new AiGenerationException("Failed to store generated course tags.", exception);
        }
    }

    private String writeObjectivesJson(List<String> objectives) {
        try {
            return objectMapper.writeValueAsString(normalizeTextList(objectives));
        } catch (JsonProcessingException exception) {
            throw new AiGenerationException("Failed to store generated lesson objectives.", exception);
        }
    }

    private String writeContentJson(List<Map<String, Object>> content) {
        try {
            return objectMapper.writeValueAsString(normalizeContentBlocks(content));
        } catch (JsonProcessingException exception) {
            throw new AiGenerationException("Failed to store generated lesson content.", exception);
        }
    }

    private List<String> readTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(tagsJson, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private List<String> readObjectives(String objectivesJson) {
        if (objectivesJson == null || objectivesJson.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(objectivesJson, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private List<Map<String, Object>> readContent(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            return List.of();
        }

        try {
            return normalizeContentBlocks(objectMapper.readValue(contentJson, CONTENT_BLOCK_LIST_TYPE));
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private List<Map<String, Object>> normalizeContentBlocks(List<Map<String, Object>> content) {
        if (content == null) {
            return List.of();
        }

        List<Map<String, Object>> normalizedContent = new ArrayList<>();
        for (Map<String, Object> block : content) {
            if (block == null) {
                continue;
            }

            Map<String, Object> normalizedBlock = new LinkedHashMap<>(block);
            Object blockType = normalizedBlock.get("type");
            if (blockType instanceof String type) {
                normalizedBlock.put("type", type.trim().toLowerCase());
            }

            if ("code".equals(normalizedBlock.get("type")) && isBlank(asString(normalizedBlock.get("text")))) {
                Object codeText = firstPresent(
                        normalizedBlock.get("code"),
                        normalizedBlock.get("source"),
                        normalizedBlock.get("sourceCode"),
                        normalizedBlock.get("content")
                );
                if (codeText != null && !isBlank(String.valueOf(codeText))) {
                    normalizedBlock.put("text", String.valueOf(codeText));
                } else {
                    normalizedBlock.put("type", "paragraph");
                    normalizedBlock.remove("language");
                    normalizedBlock.put("text", "Code example was not available for this section.");
                }
            }

            normalizedContent.add(normalizedBlock);
        }

        return normalizedContent;
    }

    private Object firstPresent(Object... values) {
        for (Object value : values) {
            if (value != null && !isBlank(String.valueOf(value))) {
                return value;
            }
        }

        return null;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }

        return String.valueOf(value);
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }

        List<String> normalizedTags = new ArrayList<>();
        for (String tag : tags) {
            if (isBlank(tag)) {
                continue;
            }

            String normalizedTag = tag.trim().toLowerCase();
            if (!normalizedTags.contains(normalizedTag)) {
                normalizedTags.add(normalizedTag);
            }
        }

        return normalizedTags;
    }

    private List<String> normalizeTextList(List<String> values) {
        if (values == null) {
            return List.of();
        }

        List<String> normalizedValues = new ArrayList<>();
        for (String value : values) {
            if (isBlank(value)) {
                continue;
            }

            String normalizedValue = value.trim();
            if (!normalizedValues.contains(normalizedValue)) {
                normalizedValues.add(normalizedValue);
            }
        }

        return normalizedValues;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
