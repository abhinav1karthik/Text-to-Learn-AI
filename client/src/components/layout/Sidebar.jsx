import { NavLink } from 'react-router-dom';
import AuthStatus from '../auth/AuthStatus.jsx';
import { useAuth } from '../../hooks/useAuth.js';
import { useAppContext } from '../../hooks/useAppContext.js';
import { ROUTES } from '../../utils/routes.js';

function navClassName({ isActive }) {
  return [
    'inline-flex min-h-11 items-center rounded-lg px-4 py-2.5 text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2',
    isActive
      ? 'bg-blue-50 text-blue-700 dark:bg-blue-950/50 dark:text-blue-300'
      : 'text-slate-600 hover:bg-slate-100 hover:text-slate-950 dark:text-slate-300 dark:hover:bg-slate-900 dark:hover:text-white',
  ].join(' ');
}

export default function Sidebar() {
  const { isAuthenticated } = useAuth();
  const { sidebarContent } = useAppContext();

  return (
    <aside className="flex gap-4 overflow-x-auto border-b border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-950 md:sticky md:top-0 md:h-screen md:flex-col md:gap-6 md:overflow-hidden md:border-b-0 md:border-r md:p-6">
      <div className="flex shrink-0 flex-col gap-6">
        <NavLink
          to={ROUTES.home}
          className="flex min-w-fit items-center gap-3 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
        >
          <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-blue-600 text-lg font-bold text-white shadow-sm">
            T
          </span>
          <span className="hidden leading-tight sm:block">
            <strong className="block text-base font-bold text-slate-950 dark:text-white">Text To Learn</strong>
            <small className="block text-sm font-medium text-slate-500 dark:text-slate-400">AI course builder</small>
          </span>
        </NavLink>

        <nav className="flex shrink-0 items-center gap-2 md:flex-col md:items-stretch" aria-label="Primary navigation">
          <NavLink className={navClassName} to={ROUTES.home}>
            Home
          </NavLink>
          {isAuthenticated ? (
            <NavLink className={navClassName} to={ROUTES.courses}>
              My courses
            </NavLink>
          ) : null}
          {!isAuthenticated ? (
            <NavLink className={navClassName} to={ROUTES.login}>
              Log in
            </NavLink>
          ) : null}
          {!isAuthenticated ? (
            <NavLink className={navClassName} to={ROUTES.signup}>
              Sign up
            </NavLink>
          ) : null}
        </nav>
      </div>

      {sidebarContent ? <div className="hidden min-h-0 flex-1 md:flex">{sidebarContent}</div> : null}

      <div className="ml-auto min-w-[220px] shrink-0 md:ml-0 md:min-w-0">
        <AuthStatus />
      </div>
    </aside>
  );
}
