import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import ErrorMessage from '../components/ui/ErrorMessage.jsx';
import LoadingSpinner from '../components/ui/LoadingSpinner.jsx';
import { useApiClient } from '../hooks/useApiClient.js';
import { ROUTES } from '../utils/routes.js';

export default function CourseDetailPage() {
  const { courseId } = useParams();
  const apiClient = useApiClient();
  const [course, setCourse] = useState(null);
  const [error, setError] = useState('');
  const [isLoading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function loadCourse() {
      setLoading(true);
      setError('');

      try {
        const courseResponse = await apiClient(`/api/courses/${courseId}`);
        if (!cancelled) {
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

    loadCourse();

    return () => {
      cancelled = true;
    };
  }, [apiClient, courseId]);

  if (isLoading) {
    return <LoadingSpinner label="Loading course outline" />;
  }

  if (error) {
    return <ErrorMessage message={error} title="Could not load course" />;
  }

  if (!course) {
    return <ErrorMessage message="Course details are not available." title="Course not found" />;
  }

  return (
    <section className="page-stack">
      <div className="course-hero">
        <div>
          <p className="eyebrow">Course</p>
          <h1>{course.title}</h1>
          <p className="lead">{course.description}</p>
          <div className="tag-row">
            {course.tags.map((tag) => (
              <span key={tag}>{tag}</span>
            ))}
          </div>
        </div>
        <div className="course-stats">
          <span>
            <strong>{course.modules.length}</strong>
            Modules
          </span>
          <span>
            <strong>{countLessons(course)}</strong>
            Lessons
          </span>
          <span>
            <strong>{countGeneratedLessons(course)}</strong>
            Generated
          </span>
        </div>
      </div>

      <div className="outline-list">
        {course.modules.map((module, moduleIndex) => (
          <article className="module-card" key={module.id}>
            <div className="module-card-header">
              <span>Module {module.position}</span>
              <strong>
                {countGeneratedLessonsInModule(module)} / {module.lessons.length} ready
              </strong>
            </div>
            <h2>{module.title}</h2>
            <p>{module.summary}</p>
            <div className="lesson-link-list">
              {module.lessons.map((lesson, lessonIndex) => (
                <Link key={lesson.id} to={ROUTES.courseLesson(course.id, moduleIndex, lessonIndex)}>
                  <span>{lesson.position}. {lesson.title}</span>
                  <small className={`lesson-status ${lesson.status.toLowerCase()}`}>
                    {lesson.status === 'GENERATED' ? 'Ready' : 'Generate'}
                  </small>
                </Link>
              ))}
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

function countLessons(course) {
  return course.modules.reduce((total, module) => total + module.lessons.length, 0);
}

function countGeneratedLessons(course) {
  return course.modules.reduce(
    (total, module) => total + countGeneratedLessonsInModule(module),
    0,
  );
}

function countGeneratedLessonsInModule(module) {
  return module.lessons.filter((lesson) => lesson.status === 'GENERATED').length;
}
