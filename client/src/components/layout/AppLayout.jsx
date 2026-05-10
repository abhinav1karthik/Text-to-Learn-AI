import { NavLink, Outlet } from 'react-router-dom';
import AuthStatus from '../auth/AuthStatus.jsx';
import { ROUTES } from '../../utils/routes.js';

export default function AppLayout() {
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <NavLink to={ROUTES.home} className="brand">
          <span className="brand-mark">T</span>
          <span>
            <strong>Text To Learn</strong>
            <small>AI course builder</small>
          </span>
        </NavLink>
        <nav className="nav-list" aria-label="Primary navigation">
          <NavLink to={ROUTES.home}>Home</NavLink>
          <NavLink to={ROUTES.login}>Log in</NavLink>
          <NavLink to={ROUTES.signup}>Sign up</NavLink>
          <NavLink to={ROUTES.course('demo-course')}>Course Preview</NavLink>
          <NavLink to={ROUTES.lesson('demo-lesson')}>Lesson Preview</NavLink>
        </nav>
        <AuthStatus />
      </aside>
      <main className="content-panel">
        <Outlet />
      </main>
    </div>
  );
}
