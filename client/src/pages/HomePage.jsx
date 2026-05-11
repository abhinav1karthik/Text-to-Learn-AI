import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import ErrorMessage from '../components/ui/ErrorMessage.jsx';
import Button from '../components/ui/Button.jsx';
import TextInput from '../components/ui/TextInput.jsx';
import LoadingSpinner from '../components/ui/LoadingSpinner.jsx';
import { useApiClient } from '../hooks/useApiClient.js';
import { useAuth } from '../hooks/useAuth.js';
import { ROUTES } from '../utils/routes.js';

export default function HomePage() {
  const [topic, setTopic] = useState('Segment Trees and Its Applications');
  const [error, setError] = useState('');
  const [isSubmitting, setSubmitting] = useState(false);
  const [recentCourses, setRecentCourses] = useState([]);
  const [isLoadingCourses, setLoadingCourses] = useState(false);
  const apiClient = useApiClient();
  const navigate = useNavigate();
  const { isAuthenticated, loginWithRedirect } = useAuth();

  useEffect(() => {
    if (!isAuthenticated) {
      setRecentCourses([]);
      return;
    }

    let cancelled = false;

    async function loadRecentCourses() {
      setLoadingCourses(true);

      try {
        const courses = await apiClient('/api/courses');
        if (!cancelled) {
          setRecentCourses(courses.slice(0, 3));
        }
      } catch {
        if (!cancelled) {
          setRecentCourses([]);
        }
      } finally {
        if (!cancelled) {
          setLoadingCourses(false);
        }
      }
    }

    loadRecentCourses();

    return () => {
      cancelled = true;
    };
  }, [apiClient, isAuthenticated]);

  async function handleSubmit(event) {
    event.preventDefault();

    const trimmedTopic = topic.trim();
    if (!trimmedTopic) {
      setError('Enter a topic before generating a course.');
      return;
    }

    if (!isAuthenticated) {
      await loginWithRedirect();
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      const course = await apiClient('/api/courses', {
        method: 'POST',
        body: JSON.stringify({ topic: trimmedTopic }),
      });
      navigate(ROUTES.course(course.id));
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="page-stack">
      <div>
        <p className="eyebrow">Course builder</p>
        <h1>Generate a course from any topic</h1>
        <p className="lead">
          Turn a topic into a structured learning path with modules, lessons, and guided
          explanations.
        </p>
      </div>

      <form className="prompt-panel" onSubmit={handleSubmit}>
        <ErrorMessage message={error} title="Course generation failed" />
        <div className="prompt-row">
          <TextInput
            id="topic"
            label="Topic prompt"
            type="text"
            placeholder="Segment Trees and Its Applications"
            value={topic}
            onChange={(event) => setTopic(event.target.value)}
            disabled={isSubmitting}
          />
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Generating...' : 'Generate'}
          </Button>
        </div>
        <p>
          Enter a topic such as data structures, guitar basics, or driving skills to create
          a personalized course outline.
        </p>
      </form>

      {isAuthenticated ? (
        <section className="recent-courses">
          <div className="section-heading-row">
            <div>
              <p className="eyebrow">Continue learning</p>
              <h2>Recent courses</h2>
            </div>
            <Link className="text-link" to={ROUTES.courses}>
              View all
            </Link>
          </div>

          {isLoadingCourses ? <LoadingSpinner label="Loading saved courses" /> : null}

          {!isLoadingCourses && recentCourses.length === 0 ? (
            <p className="muted-text">Your generated courses will appear here.</p>
          ) : null}

          {!isLoadingCourses && recentCourses.length > 0 ? (
            <div className="mini-course-list">
              {recentCourses.map((course) => (
                <Link key={course.id} to={ROUTES.course(course.id)}>
                  <strong>{course.title}</strong>
                  <span>
                    {course.modules.length} modules / {countLessons(course)} lessons
                  </span>
                </Link>
              ))}
            </div>
          ) : null}
        </section>
      ) : null}
    </section>
  );
}

function countLessons(course) {
  return course.modules.reduce((total, module) => total + module.lessons.length, 0);
}
