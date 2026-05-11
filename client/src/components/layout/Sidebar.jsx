import { NavLink } from 'react-router-dom';
import AuthStatus from '../auth/AuthStatus.jsx';
import { useAuth } from '../../hooks/useAuth.js';
import { useAppContext } from '../../hooks/useAppContext.js';
import { ROUTES } from '../../utils/routes.js';

export default function Sidebar() {
  const { isAuthenticated } = useAuth();
  const { sidebarContent } = useAppContext();

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
        {isAuthenticated ? <NavLink to={ROUTES.courses}>My courses</NavLink> : null}
        {!isAuthenticated ? <NavLink to={ROUTES.login}>Log in</NavLink> : null}
        {!isAuthenticated ? <NavLink to={ROUTES.signup}>Sign up</NavLink> : null}
      </nav>

      {sidebarContent}

      <AuthStatus />
    </aside>
  );
}
