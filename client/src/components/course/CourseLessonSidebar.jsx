import { Link } from 'react-router-dom';
import { ROUTES } from '../../utils/routes.js';
import StatusBadge from '../ui/StatusBadge.jsx';

export default function CourseLessonSidebar({ course, activeModuleIndex = null, activeLessonIndex = null }) {
  if (!course) {
    return null;
  }

  return (
    <section
      className="hidden min-h-0 flex-1 flex-col gap-4 overflow-hidden rounded-xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-800 dark:bg-slate-900 md:flex"
      aria-label="Course lessons"
    >
      <Link
        className="text-sm font-semibold text-blue-600 hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:text-blue-400 dark:hover:text-blue-300"
        to={ROUTES.course(course.id)}
      >
        Course home page
      </Link>
      <Link
        className="rounded-lg text-sm font-semibold leading-snug text-slate-900 no-underline hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:text-slate-100 dark:hover:text-blue-300"
        to={ROUTES.course(course.id)}
      >
        {course.title}
      </Link>
      <div className="min-h-0 flex-1 overflow-y-auto pr-1">
        <div className="flex flex-col gap-4">
          {course.modules.map((module, moduleIndex) => (
            <div className="flex flex-col gap-1" key={module.id}>
              <strong className="mb-1 block text-xs font-bold uppercase tracking-wide text-slate-700 dark:text-slate-300">
                {module.title}
              </strong>
              {module.lessons.map((lesson, lessonIndex) => {
                const isActive = moduleIndex === activeModuleIndex && lessonIndex === activeLessonIndex;
                return (
                  <Link
                    className={[
                      'relative flex min-h-10 items-center rounded-lg border border-transparent py-2 pl-2.5 pr-5 text-xs no-underline transition-colors hover:bg-blue-50 hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:hover:bg-blue-950/40 dark:hover:text-blue-300',
                      isActive
                        ? 'bg-blue-50 font-semibold text-blue-700 dark:bg-blue-950/50 dark:text-blue-300'
                        : 'text-slate-500 dark:text-slate-400',
                    ].join(' ')}
                    key={lesson.id}
                    to={ROUTES.courseLesson(course.id, moduleIndex, lessonIndex)}
                  >
                    <span className="truncate">{lesson.title}</span>
                    <StatusBadge status={lesson.status} />
                  </Link>
                );
              })}
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
