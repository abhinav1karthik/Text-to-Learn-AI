import { useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth.js';

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
    <header className="topbar">
      <div>
        <p className="topbar-kicker">Workspace</p>
        <h2>{getPageTitle(pathname)}</h2>
      </div>
      <div className="topbar-user">
        <span>{isAuthenticated ? user?.name || user?.email || 'Signed in' : 'Guest'}</span>
      </div>
    </header>
  );
}
