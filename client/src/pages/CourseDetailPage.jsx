import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import CourseLessonSidebar from '../components/course/CourseLessonSidebar.jsx';
import ErrorMessage from '../components/ui/ErrorMessage.jsx';
import LoadingSpinner from '../components/ui/LoadingSpinner.jsx';
import StatusBadge from '../components/ui/StatusBadge.jsx';
import { useApiClient } from '../hooks/useApiClient.js';
import { useAppContext } from '../hooks/useAppContext.js';
import { ROUTES } from '../utils/routes.js';

export default function CourseDetailPage() {
  const { courseId } = useParams();
  const apiClient = useApiClient();
  const { setSidebarContent } = useAppContext();
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

  useEffect(() => {
    if (!course) {
      setSidebarContent(null);
      return undefined;
    }

    setSidebarContent(<CourseLessonSidebar course={course} />);

    return () => {
      setSidebarContent(null);
    };
  }, [course, setSidebarContent]);

  if (isLoading) {
    return <LoadingSpinner label="Loading course home page" />;
  }

  if (error) {
    return <ErrorMessage message={error} title="Could not load course" />;
  }

  if (!course) {
    return <ErrorMessage message="Course details are not available." title="Course not found" />;
  }

  return (
    <section className="flex max-w-[1100px] flex-col gap-6">
      <div className="flex flex-col items-start justify-between gap-6 rounded-xl border border-slate-200 bg-white p-7 dark:border-slate-800 dark:bg-slate-900 lg:flex-row">
        <div>
          <p className="mb-2 text-xs font-bold uppercase tracking-widest text-blue-600">Course</p>
          <h1 className="text-4xl font-bold leading-tight text-slate-900 dark:text-white">{course.title}</h1>
          <p className="mt-3 max-w-2xl text-lg leading-relaxed text-slate-500 dark:text-slate-400">{course.description}</p>
          <div className="mt-4 flex flex-wrap gap-2">
            {course.tags.map((tag) => (
              <span
                className="rounded-full border border-blue-200 bg-blue-50 px-2.5 py-1 text-xs font-semibold text-blue-700 dark:border-blue-900 dark:bg-blue-950/50 dark:text-blue-300"
                key={tag}
              >
                {tag}
              </span>
            ))}
          </div>
        </div>
        <div className="grid w-full grid-cols-3 gap-3 lg:min-w-[260px] lg:w-auto">
          <span className="flex flex-col gap-1 rounded-lg border border-slate-200 bg-slate-50 p-3 text-center text-xs font-semibold uppercase tracking-wide text-slate-500 dark:border-slate-800 dark:bg-slate-950 dark:text-slate-400">
            <strong className="text-2xl font-bold text-slate-900 dark:text-white">{course.modules.length}</strong>
            Modules
          </span>
          <span className="flex flex-col gap-1 rounded-lg border border-slate-200 bg-slate-50 p-3 text-center text-xs font-semibold uppercase tracking-wide text-slate-500 dark:border-slate-800 dark:bg-slate-950 dark:text-slate-400">
            <strong className="text-2xl font-bold text-slate-900 dark:text-white">{countLessons(course)}</strong>
            Lessons
          </span>
          <span className="flex flex-col gap-1 rounded-lg border border-slate-200 bg-slate-50 p-3 text-center text-xs font-semibold uppercase tracking-wide text-slate-500 dark:border-slate-800 dark:bg-slate-950 dark:text-slate-400">
            <strong className="text-2xl font-bold text-slate-900 dark:text-white">{countGeneratedLessons(course)}</strong>
            Built
          </span>
        </div>
      </div>

      <div className="flex flex-col gap-3">
        {course.modules.map((module, moduleIndex) => (
          <article className="rounded-xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900" key={module.id}>
            <div className="mb-3 flex items-center justify-between gap-4">
              <span className="text-xs font-bold uppercase tracking-wide text-blue-600">Module {module.position}</span>
              <strong className="text-xs font-semibold text-slate-500 dark:text-slate-400">
                {countGeneratedLessonsInModule(module)} / {module.lessons.length} built
              </strong>
            </div>
            <h2 className="text-xl font-semibold text-slate-900 dark:text-white">{module.title}</h2>
            <p className="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">{module.summary}</p>
            <div className="mt-4 flex flex-col gap-2">
              {module.lessons.map((lesson, lessonIndex) => (
                <Link
                  className="relative flex items-center justify-between gap-3 overflow-hidden rounded-lg border border-slate-100 bg-slate-50 py-2.5 pl-3 pr-6 no-underline transition-colors hover:border-blue-300 hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-800 dark:bg-slate-950 dark:hover:border-blue-800 dark:hover:bg-blue-950/30"
                  key={lesson.id}
                  to={ROUTES.courseLesson(course.id, moduleIndex, lessonIndex)}
                >
                  <span className="text-sm font-medium text-slate-700 dark:text-slate-300">{lesson.position}. {lesson.title}</span>
                  <StatusBadge status={lesson.status} />
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
