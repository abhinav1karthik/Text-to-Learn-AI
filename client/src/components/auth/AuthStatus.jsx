import { useEffect, useState } from 'react';
import LoginButton from './LoginButton.jsx';
import LogoutButton from './LogoutButton.jsx';
import { useApiClient } from '../../hooks/useApiClient.js';
import { useAuth } from '../../hooks/useAuth.js';

export default function AuthStatus() {
  const { isAuthenticated, isConfigured, isLoading, user } = useAuth();
  const apiClient = useApiClient();
  const [syncStatus, setSyncStatus] = useState('idle');

  useEffect(() => {
    if (!isConfigured || isLoading || !isAuthenticated) {
      setSyncStatus('idle');
      return;
    }

    let cancelled = false;
    setSyncStatus('syncing');

    apiClient('/api/users/me')
      .then(() => {
        if (!cancelled) {
          setSyncStatus('synced');
        }
      })
      .catch(() => {
        if (!cancelled) {
          setSyncStatus('failed');
        }
      });

    return () => {
      cancelled = true;
    };
  }, [apiClient, isAuthenticated, isConfigured, isLoading]);

  if (!isConfigured) {
    return (
      <div className="auth-card">
        <strong>Authentication setup required</strong>
        <small>Add your Auth0 values to `client/.env.local`.</small>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="auth-card">
        <strong>Checking session</strong>
        <small>Please wait.</small>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="auth-card">
        <strong>Guest session</strong>
        <small>Log in to save courses to your account.</small>
        <div className="auth-actions">
          <LoginButton />
        </div>
      </div>
    );
  }

  return (
    <div className="auth-card">
      <strong>{user?.name ?? user?.email ?? 'Signed in'}</strong>
      <small>{user?.email}</small>
      {syncStatus === 'syncing' ? <small>Syncing account</small> : null}
      {syncStatus === 'synced' ? <small>Account synced</small> : null}
      {syncStatus === 'failed' ? <small>Account sync failed</small> : null}
      <div className="auth-actions">
        <LogoutButton />
      </div>
    </div>
  );
}
