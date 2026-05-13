import { Outlet, useLocation } from 'react-router-dom';
import { useAppContext } from '../../hooks/useAppContext.js';
import ErrorMessage from '../ui/ErrorMessage.jsx';
import LoadingSpinner from '../ui/LoadingSpinner.jsx';
import Sidebar from './Sidebar.jsx';
import Topbar from './Topbar.jsx';

export default function AppLayout() {
  const { pathname } = useLocation();
  const { globalError, isGlobalLoading, sidebarContent } = useAppContext();
  const isLessonPage = pathname.includes('/lesson/');
  const shellColumns = sidebarContent ? 'md:grid-cols-[360px_minmax(0,1fr)]' : 'md:grid-cols-[280px_minmax(0,1fr)]';
  const contentWidth = isLessonPage ? 'max-w-[1040px]' : 'max-w-[1100px]';

  return (
    <div className={`grid min-h-screen grid-cols-1 bg-slate-50 text-slate-950 dark:bg-slate-950 dark:text-slate-100 ${shellColumns}`}>
      <Sidebar />
      <div className="flex min-w-0 flex-col">
        <Topbar />
        <main className={`mx-auto flex w-full ${contentWidth} flex-1 flex-col gap-6 px-5 py-6 sm:px-6 md:px-10 md:py-10`}>
          {isGlobalLoading ? <LoadingSpinner label="Preparing your workspace" /> : null}
          <ErrorMessage message={globalError} title="Workspace issue" />
          <Outlet />
        </main>
      </div>
    </div>
  );
}
