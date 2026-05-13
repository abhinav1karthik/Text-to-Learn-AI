import { useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth.js';
import ThemeToggle from './ThemeToggle.jsx';

function getPageTitle(pathname) {
  if (pathname === '/') {
    return 'Course Builder';
  }

  if (pathname === '/courses') {
    return 'My Courses';
  }

  if (pathname.startsWith('/courses/') && pathname.includes('/lesson/')) {
    return 'Lesson Viewer';
  }

  if (pathname.startsWith('/courses/')) {
    return 'Course Overview';
  }

  if (pathname === '/login') {
    return 'Log In';
  }

  if (pathname === '/signup') {
    return 'Sign Up';
  }

  return 'Text To Learn';
}

export default function Topbar() {
  const { pathname } = useLocation();
  const { isAuthenticated, user } = useAuth();

  return (
    <header className="flex items-center justify-between gap-4 border-b border-slate-200 bg-white px-5 py-4 dark:border-slate-800 dark:bg-slate-950 sm:px-6 md:px-10">
      <div>
        <p className="mb-0.5 text-xs font-bold uppercase tracking-widest text-blue-600">Workspace</p>
        <h1 className="m-0 text-lg font-semibold text-slate-900 dark:text-white">{getPageTitle(pathname)}</h1>
      </div>
      <div className="flex items-center gap-3">
        <ThemeToggle />
        <div className="max-w-[220px] truncate rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-sm text-slate-600 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300">
          <span>{isAuthenticated ? user?.name || user?.email || 'Signed in' : 'Guest'}</span>
        </div>
      </div>
    </header>
  );
}
