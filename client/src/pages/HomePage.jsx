import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import ErrorMessage from '../components/ui/ErrorMessage.jsx';
import Button from '../components/ui/Button.jsx';
import TextInput from '../components/ui/TextInput.jsx';
import LoadingSpinner from '../components/ui/LoadingSpinner.jsx';
import { useApiClient } from '../hooks/useApiClient.js';
import { useAuth } from '../hooks/useAuth.js';
import { ROUTES } from '../utils/routes.js';

const ACTIVE_COURSE_JOB_KEY = 'text-to-learn:activeCourseGenerationJobId';
const COURSE_JOB_POLL_INTERVAL_MS = 2000;

export default function HomePage() {
  const [topic, setTopic] = useState('Segment Trees and Its Applications');
  const [error, setError] = useState('');
  const [isSubmitting, setSubmitting] = useState(false);
  const [activeJobId, setActiveJobId] = useState(() =>
    window.localStorage.getItem(ACTIVE_COURSE_JOB_KEY),
  );
  const [activeJob, setActiveJob] = useState(null);
  const [recentCourses, setRecentCourses] = useState([]);
  const [isLoadingCourses, setLoadingCourses] = useState(false);
  const apiClient = useApiClient();
  const navigate = useNavigate();
  const { isAuthenticated, loginWithRedirect } = useAuth();
  const isGenerationInProgress = isSubmitting || Boolean(activeJobId);

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

  useEffect(() => {
    if (!isAuthenticated || !activeJobId) {
      return undefined;
    }

    let cancelled = false;
    let timeoutId;

    async function pollJob() {
      try {
        const job = await apiClient(`/api/generation-jobs/${activeJobId}`);
        if (cancelled) {
          return;
        }

        setActiveJob(job);

        if (job.status === 'SUCCEEDED' && job.courseId) {
          clearActiveJob();
          navigate(ROUTES.course(job.courseId));
          return;
        }

        if (job.status === 'FAILED') {
          clearActiveJob();
          setError(job.errorMessage || 'Course generation failed. Please try again.');
          return;
        }

        timeoutId = window.setTimeout(pollJob, COURSE_JOB_POLL_INTERVAL_MS);
      } catch (requestError) {
        if (!cancelled) {
          clearActiveJob();
          setError(requestError.message);
        }
      }
    }

    pollJob();

    return () => {
      cancelled = true;
      window.clearTimeout(timeoutId);
    };
  }, [activeJobId, apiClient, isAuthenticated, navigate]);

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
      const job = await apiClient('/api/generation-jobs/course', {
        method: 'POST',
        body: JSON.stringify({ topic: trimmedTopic }),
      });
      setActiveJob(job);
      setActiveJobId(job.id);
      window.localStorage.setItem(ACTIVE_COURSE_JOB_KEY, job.id);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSubmitting(false);
    }
  }

  function clearActiveJob() {
    setActiveJobId(null);
    setActiveJob(null);
    window.localStorage.removeItem(ACTIVE_COURSE_JOB_KEY);
  }

  return (
    <section className="flex max-w-[1100px] flex-col gap-7">
      <div>
        <p className="mb-2 text-xs font-bold uppercase tracking-widest text-blue-600">Course builder</p>
        <h1 className="mb-3 text-4xl font-bold leading-tight text-slate-900 dark:text-white">Generate a course from any topic</h1>
        <p className="max-w-2xl text-lg leading-relaxed text-slate-500 dark:text-slate-400">
          Turn a topic into a structured learning path with modules, lessons, and guided
          explanations.
        </p>
      </div>

      <form className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900" onSubmit={handleSubmit}>
        <ErrorMessage message={error} title="Course generation failed" />
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <div className="min-w-0 flex-1">
            <TextInput
              id="topic"
              label="Topic prompt"
              type="text"
              placeholder="Segment Trees and Its Applications"
              value={topic}
              onChange={(event) => setTopic(event.target.value)}
              disabled={isGenerationInProgress}
            />
          </div>
          <Button className="sm:w-auto" type="submit" disabled={isGenerationInProgress}>
            {isGenerationInProgress ? 'Preparing...' : 'Generate'}
          </Button>
        </div>
        {activeJobId ? (
          <div className="rounded-lg border border-blue-100 bg-blue-50 p-4 dark:border-blue-900/70 dark:bg-blue-950/30">
            <LoadingSpinner label={jobStatusLabel(activeJob)} />
            <p className="mt-3 text-sm leading-6 text-slate-600 dark:text-slate-300">
              You can keep this page open while the backend generates the course. The page
              will move to the course automatically when the outline is ready.
            </p>
          </div>
        ) : null}
        <p className="text-sm leading-6 text-slate-500 dark:text-slate-400">
          Enter a topic such as data structures, guitar basics, or driving skills to create
          a personalized course home page.
        </p>
      </form>

      {isAuthenticated ? (
        <section className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="mb-2 text-xs font-bold uppercase tracking-widest text-blue-600">Continue learning</p>
              <h2 className="text-xl font-semibold text-slate-900 dark:text-white">Recent courses</h2>
            </div>
            <Link
              className="rounded-lg px-3 py-2 text-sm font-semibold text-blue-600 transition hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:text-blue-400 dark:hover:bg-blue-950/40"
              to={ROUTES.courses}
            >
              View all
            </Link>
          </div>

          {isLoadingCourses ? <LoadingSpinner label="Loading saved courses" /> : null}

          {!isLoadingCourses && recentCourses.length === 0 ? (
            <p className="text-sm leading-6 text-slate-500 dark:text-slate-400">Your generated courses will appear here.</p>
          ) : null}

          {!isLoadingCourses && recentCourses.length > 0 ? (
            <div className="flex flex-col gap-3">
              {recentCourses.map((course) => (
                <Link
                  className="flex items-center justify-between gap-4 rounded-lg border border-slate-100 bg-slate-50 px-4 py-3 no-underline transition-colors hover:border-blue-300 hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-800 dark:bg-slate-950 dark:hover:border-blue-800 dark:hover:bg-blue-950/30"
                  key={course.id}
                  to={ROUTES.course(course.id)}
                >
                  <strong className="text-sm font-semibold text-slate-900 dark:text-white">{course.title}</strong>
                  <span className="shrink-0 text-xs font-semibold text-slate-500 dark:text-slate-400">
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

function jobStatusLabel(job) {
  if (!job) {
    return 'Queued course generation';
  }

  if (job.status === 'RUNNING') {
    return 'Generating course outline';
  }

  if (job.status === 'QUEUED') {
    return 'Waiting for generation worker';
  }

  return 'Preparing course';
}
