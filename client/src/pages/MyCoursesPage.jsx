import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import EmptyState from '../components/ui/EmptyState.jsx';
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
    <section className="flex max-w-[1100px] flex-col gap-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="mb-2 text-xs font-bold uppercase tracking-widest text-blue-600">Library</p>
          <h1 className="text-4xl font-bold leading-tight text-slate-900 dark:text-white">Your courses</h1>
          <p className="mt-3 max-w-2xl text-lg leading-relaxed text-slate-500 dark:text-slate-400">
            Continue a course home page, open a lesson, or generate a new path.
          </p>
        </div>
        <Link
          className="inline-flex items-center justify-center rounded-lg bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          to={ROUTES.home}
        >
          New course
        </Link>
      </div>

      {courses.length === 0 ? (
        <EmptyState
          heading="No courses yet"
          body="Create your first course from a topic prompt and it will appear here."
          linkTo={ROUTES.home}
          linkLabel="Create course"
        />
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {courses.map((course) => (
            <Link
              className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-5 no-underline transition-all hover:border-blue-400 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-800 dark:bg-slate-900 dark:hover:border-blue-700"
              key={course.id}
              to={ROUTES.course(course.id)}
            >
              <div>
                <h2 className="mb-1 mt-2 text-base font-semibold text-slate-900 dark:text-white">{course.title}</h2>
                <p className="text-sm leading-relaxed text-slate-500 dark:text-slate-400">{course.description}</p>
              </div>
              <div className="flex flex-wrap gap-2">
                <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                  {course.modules.length} modules
                </span>
                <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                  {countLessons(course)} lessons
                </span>
              </div>
              <div className="flex flex-wrap gap-2">
                {course.tags.slice(0, 4).map((tag) => (
                  <span
                    className="rounded-full border border-blue-200 bg-blue-50 px-2.5 py-1 text-xs font-semibold text-blue-700 dark:border-blue-900 dark:bg-blue-950/50 dark:text-blue-300"
                    key={tag}
                  >
                    {tag}
                  </span>
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
