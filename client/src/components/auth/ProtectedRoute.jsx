import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth.js';

export default function ProtectedRoute({ children }) {
  const location = useLocation();
  const { isAuthenticated, isConfigured, isLoading } = useAuth();

  if (!isConfigured) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (isLoading) {
    return (
      <section className="flex flex-col gap-2">
        <p className="text-xs font-bold uppercase tracking-widest text-blue-600">Session</p>
        <h1 className="text-3xl font-bold text-slate-900">Checking your session</h1>
      </section>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return children;
}
