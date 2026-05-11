import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import ErrorMessage from '../components/ui/ErrorMessage.jsx';
import LoadingSpinner from '../components/ui/LoadingSpinner.jsx';
import { useApiClient } from '../hooks/useApiClient.js';
import { ROUTES } from '../utils/routes.js';

export default function MyCoursesPage() {
  const apiClient = useApiClient();
  const [courses, setCourses] = useState([]);
  const [error, setError] = useState('');
  const [isLoading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function loadCourses() {
      setLoading(true);
      setError('');

      try {
        const courseResponse = await apiClient('/api/courses');
        if (!cancelled) {
          setCourses(courseResponse);
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

    loadCourses();

    return () => {
      cancelled = true;
    };
  }, [apiClient]);

  if (isLoading) {
    return <LoadingSpinner label="Loading your courses" />;
  }

  if (error) {
    return <ErrorMessage message={error} title="Could not load courses" />;
  }

  return (
    <section className="page-stack">
      <div className="page-heading-row">
        <div>
          <p className="eyebrow">Library</p>
          <h1>Your courses</h1>
          <p className="lead">Continue a course outline, open a lesson, or generate a new path.</p>
        </div>
        <Link className="button button-link" to={ROUTES.home}>
          New course
        </Link>
      </div>

      {courses.length === 0 ? (
        <div className="empty-library">
          <h2>No courses yet</h2>
          <p>Create your first course from a topic prompt and it will appear here.</p>
          <Link className="button button-link" to={ROUTES.home}>
            Create course
          </Link>
        </div>
      ) : (
        <div className="course-card-grid">
          {courses.map((course) => (
            <Link className="course-card" key={course.id} to={ROUTES.course(course.id)}>
              <div>
                <span className="course-status">{course.status.replaceAll('_', ' ')}</span>
                <h2>{course.title}</h2>
                <p>{course.description}</p>
              </div>
              <div className="course-card-meta">
                <span>{course.modules.length} modules</span>
                <span>{countLessons(course)} lessons</span>
              </div>
              <div className="tag-row compact-tags">
                {course.tags.slice(0, 4).map((tag) => (
                  <span key={tag}>{tag}</span>
                ))}
              </div>
            </Link>
          ))}
        </div>
      )}
    </section>
  );
}

function countLessons(course) {
  return course.modules.reduce((total, module) => total + module.lessons.length, 0);
}
