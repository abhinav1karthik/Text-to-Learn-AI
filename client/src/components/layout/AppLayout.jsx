import { Outlet } from 'react-router-dom';
import { useAppContext } from '../../hooks/useAppContext.js';
import ErrorMessage from '../ui/ErrorMessage.jsx';
import LoadingSpinner from '../ui/LoadingSpinner.jsx';
import Sidebar from './Sidebar.jsx';
import Topbar from './Topbar.jsx';

export default function AppLayout() {
  const { globalError, isGlobalLoading, sidebarContent } = useAppContext();

  return (
    <div className={`app-shell ${sidebarContent ? 'has-sidebar-content' : ''}`}>
      <Sidebar />
      <div className="app-main">
        <Topbar />
        <main className="content-panel">
          {isGlobalLoading ? <LoadingSpinner label="Preparing your workspace" /> : null}
          <ErrorMessage message={globalError} />
          <Outlet />
        </main>
      </div>
    </div>
  );
}
