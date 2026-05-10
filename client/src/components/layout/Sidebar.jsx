import { NavLink } from 'react-router-dom';
import AuthStatus from '../auth/AuthStatus.jsx';
import { ROUTES } from '../../utils/routes.js';

export default function Sidebar() {
  return (
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
        <NavLink to={ROUTES.courseLesson('demo-course', 0, 0)}>Lesson Preview</NavLink>
      </nav>

      <AuthStatus />
    </aside>
  );
}
