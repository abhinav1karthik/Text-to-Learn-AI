import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import LessonAudioPlayer from '../components/lesson/LessonAudioPlayer.jsx';
import LessonRenderer from '../components/lesson/LessonRenderer.jsx';
import ErrorMessage from '../components/ui/ErrorMessage.jsx';
import LoadingSpinner from '../components/ui/LoadingSpinner.jsx';
import { useApiClient } from '../hooks/useApiClient.js';
import { useAppContext } from '../hooks/useAppContext.js';
import { ROUTES } from '../utils/routes.js';

export default function LessonDetailPage() {
  const { courseId, lessonIndex, moduleIndex } = useParams();
  const apiClient = useApiClient();
  const { setSidebarContent } = useAppContext();
  const [lesson, setLesson] = useState(null);
  const [course, setCourse] = useState(null);
  const [error, setError] = useState('');
  const [isLoading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function loadLesson() {
      setLoading(true);
      setError('');

      try {
        const lessonResponse = await apiClient(
          `/api/courses/${courseId}/module/${moduleIndex}/lesson/${lessonIndex}`,
        );
        const courseResponse = await apiClient(`/api/courses/${courseId}`);
        if (!cancelled) {
          setLesson(lessonResponse);
          setCourse(courseResponse);
        }
      } catch (requestError) {
        if (!cancelled) {
          setError(requestError.message);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadLesson();

    return () => {
      cancelled = true;
    };
  }, [apiClient, courseId, lessonIndex, moduleIndex]);

  useEffect(() => {
    if (!course) {
      setSidebarContent(null);
      return undefined;
    }

    setSidebarContent(
      <CourseLessonOutline
        course={course}
        lessonIndex={Number(lessonIndex)}
        moduleIndex={Number(moduleIndex)}
      />,
    );

    return () => {
      setSidebarContent(null);
    };
  }, [course, lessonIndex, moduleIndex, setSidebarContent]);

  if (isLoading) {
    return <LoadingSpinner label="Preparing lesson content" />;
  }

  if (error) {
    return <ErrorMessage message={error} title="Could not load lesson" />;
  }

  if (!lesson) {
    return <ErrorMessage message="Lesson details are not available." title="Lesson not found" />;
  }

  const flatLessons = course ? flattenLessons(course) : [];
  const currentIndex = flatLessons.findIndex(
    (item) => item.moduleIndex === Number(moduleIndex) && item.lessonIndex === Number(lessonIndex),
  );
  const previousLesson = currentIndex > 0 ? flatLessons[currentIndex - 1] : null;
  const nextLesson =
    currentIndex >= 0 && currentIndex < flatLessons.length - 1 ? flatLessons[currentIndex + 1] : null;

  return (
    <section className="lesson-workspace">
      <div className="lesson-main">
        <div>
          <p className="eyebrow">Lesson</p>
          <h1>{lesson.title}</h1>
          <p className="lead">
            {lesson.courseTitle} / {lesson.moduleTitle}
          </p>
        </div>

        {lesson.objectives.length > 0 && (
          <section className="lesson-objectives" aria-labelledby="lesson-objectives-title">
            <h2 id="lesson-objectives-title">Objectives</h2>
            <ul>
              {lesson.objectives.map((objective) => (
                <li key={objective}>{objective}</li>
              ))}
            </ul>
          </section>
        )}

        <LessonAudioPlayer
          courseId={courseId}
          lessonIndex={lessonIndex}
          lessonTitle={lesson.title}
          moduleIndex={moduleIndex}
        />

        <LessonRenderer content={lesson.content} />

        <nav className="lesson-pager" aria-label="Lesson navigation">
          {previousLesson ? (
            <Link to={ROUTES.courseLesson(course.id, previousLesson.moduleIndex, previousLesson.lessonIndex)}>
              <small>Previous</small>
              <span>{previousLesson.lesson.title}</span>
            </Link>
          ) : (
            <span />
          )}
          {nextLesson ? (
            <Link to={ROUTES.courseLesson(course.id, nextLesson.moduleIndex, nextLesson.lessonIndex)}>
              <small>Next</small>
              <span>{nextLesson.lesson.title}</span>
            </Link>
          ) : (
            <span />
          )}
        </nav>
      </div>
    </section>
  );
}

function CourseLessonOutline({ course, lessonIndex, moduleIndex }) {
  return (
    <section className="sidebar-course-outline" aria-label="Course lessons">
      <Link className="text-link" to={ROUTES.course(course.id)}>
        Course outline
      </Link>
      <h2>{course.title}</h2>
      <div className="lesson-outline-scroll">
        {course.modules.map((module, outlineModuleIndex) => (
          <div className="lesson-outline-module" key={module.id}>
            <strong>{module.title}</strong>
            {module.lessons.map((outlineLesson, outlineLessonIndex) => {
              const isActive =
                outlineModuleIndex === moduleIndex && outlineLessonIndex === lessonIndex;
              return (
                <Link
                  className={isActive ? 'is-active' : ''}
                  key={outlineLesson.id}
                  to={ROUTES.courseLesson(course.id, outlineModuleIndex, outlineLessonIndex)}
                >
                  <span>{outlineLesson.title}</span>
                  <small className={`lesson-status ${outlineLesson.status.toLowerCase()}`}>
                    {outlineLesson.status === 'GENERATED' ? 'Ready' : 'Plan'}
                  </small>
                </Link>
              );
            })}
          </div>
        ))}
      </div>
    </section>
  );
}

function flattenLessons(course) {
  return course.modules.flatMap((module, moduleIndex) =>
    module.lessons.map((lesson, lessonIndex) => ({
      lesson,
      lessonIndex,
      module,
      moduleIndex,
    })),
  );
}
