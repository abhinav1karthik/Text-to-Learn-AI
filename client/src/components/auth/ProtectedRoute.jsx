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
      <section className="page-stack">
        <p className="eyebrow">Session</p>
        <h1>Checking your session</h1>
      </section>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return children;
}
