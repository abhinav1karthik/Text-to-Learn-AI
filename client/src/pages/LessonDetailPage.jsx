import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import CourseLessonSidebar from '../components/course/CourseLessonSidebar.jsx';
import LessonAudioPlayer from '../components/lesson/LessonAudioPlayer.jsx';
import LessonPdfDownloadButton from '../components/lesson/LessonPdfDownloadButton.jsx';
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
          setCourse(
            withUpdatedLessonStatus(
              courseResponse,
              Number(moduleIndex),
              Number(lessonIndex),
              lessonResponse.status,
            ),
          );
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
      <CourseLessonSidebar
        course={course}
        activeLessonIndex={Number(lessonIndex)}
        activeModuleIndex={Number(moduleIndex)}
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
    <section className="flex max-w-[1040px] flex-col gap-6">
      <Link
        className="inline-flex w-fit items-center rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-600 no-underline transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300 dark:hover:border-blue-800 dark:hover:bg-blue-950/30 dark:hover:text-blue-300"
        to={ROUTES.course(courseId)}
      >
        Back to course home page
      </Link>
      <div className="flex flex-col gap-2">
        <p className="text-xs font-bold uppercase tracking-widest text-blue-600">Lesson</p>
        <h1 className="text-3xl font-bold leading-tight text-slate-900 dark:text-white">{lesson.title}</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          {lesson.courseTitle} / {lesson.moduleTitle}
        </p>
      </div>

      {lesson.objectives.length > 0 && (
        <section className="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900" aria-labelledby="lesson-objectives-title">
          <h2 className="text-xl font-semibold text-slate-900 dark:text-white" id="lesson-objectives-title">
            Objectives
          </h2>
          <ul className="mt-3 flex list-disc flex-col gap-2 pl-5">
            {lesson.objectives.map((objective) => (
              <li className="text-sm leading-relaxed text-slate-600 dark:text-slate-300" key={objective}>
                {objective}
              </li>
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

      <LessonPdfDownloadButton
        courseId={courseId}
        lessonIndex={lessonIndex}
        lessonTitle={lesson.title}
        moduleIndex={moduleIndex}
      />

      <nav className="grid grid-cols-1 gap-3 sm:grid-cols-2" aria-label="Lesson navigation">
        {previousLesson ? (
          <Link
            className="flex flex-col gap-1 rounded-xl border border-slate-200 bg-white p-4 no-underline transition-colors hover:border-blue-300 hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-800 dark:bg-slate-900 dark:hover:border-blue-800 dark:hover:bg-blue-950/30"
            to={ROUTES.courseLesson(course.id, previousLesson.moduleIndex, previousLesson.lessonIndex)}
          >
            <small className="text-xs font-bold uppercase tracking-wide text-slate-500 dark:text-slate-400">Previous</small>
            <span className="text-sm font-semibold text-slate-900 dark:text-white">{previousLesson.lesson.title}</span>
          </Link>
        ) : (
          <span />
        )}
        {nextLesson ? (
          <Link
            className="flex flex-col gap-1 rounded-xl border border-slate-200 bg-white p-4 text-right no-underline transition-colors hover:border-blue-300 hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-800 dark:bg-slate-900 dark:hover:border-blue-800 dark:hover:bg-blue-950/30"
            to={ROUTES.courseLesson(course.id, nextLesson.moduleIndex, nextLesson.lessonIndex)}
          >
            <small className="text-xs font-bold uppercase tracking-wide text-slate-500 dark:text-slate-400">Next</small>
            <span className="text-sm font-semibold text-slate-900 dark:text-white">{nextLesson.lesson.title}</span>
          </Link>
        ) : (
          <span />
        )}
      </nav>
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

function withUpdatedLessonStatus(course, moduleIndex, lessonIndex, status) {
  if (!course || !status) {
    return course;
  }

  return {
    ...course,
    modules: course.modules.map((module, currentModuleIndex) => {
      if (currentModuleIndex !== moduleIndex) {
        return module;
      }

      return {
        ...module,
        lessons: module.lessons.map((lesson, currentLessonIndex) => {
          if (currentLessonIndex !== lessonIndex) {
            return lesson;
          }

          return {
            ...lesson,
            status,
          };
        }),
      };
    }),
  };
}
