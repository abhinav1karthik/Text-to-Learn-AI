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
      <div className="flex flex-col gap-2 rounded-xl border border-amber-200 bg-amber-50 p-4 text-amber-900 dark:border-amber-900 dark:bg-amber-950/30 dark:text-amber-200">
        <strong className="block text-sm font-semibold">Authentication setup required</strong>
        <small className="block text-xs leading-5">Add your Auth0 values to `client/.env.local`.</small>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="flex flex-col gap-2 rounded-xl border border-slate-200 bg-slate-50 p-4 text-slate-600 dark:border-slate-800 dark:bg-slate-900">
        <strong className="block text-sm font-semibold text-slate-900 dark:text-white">Checking session</strong>
        <small className="block text-xs text-slate-500 dark:text-slate-400">Please wait.</small>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="flex flex-col gap-2 rounded-xl border border-slate-200 bg-slate-50 p-4 text-slate-600 dark:border-slate-800 dark:bg-slate-900">
        <strong className="block text-sm font-semibold text-slate-900 dark:text-white">Guest session</strong>
        <small className="block text-xs text-slate-500 dark:text-slate-400">Log in to save courses to your account.</small>
        <div className="mt-1 flex flex-wrap gap-2">
          <LoginButton />
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-2 rounded-xl border border-slate-200 bg-slate-50 p-4 text-slate-600 dark:border-slate-800 dark:bg-slate-900">
      <strong className="block truncate text-sm font-semibold text-slate-900 dark:text-white">{user?.name ?? user?.email ?? 'Signed in'}</strong>
      <small className="block truncate text-xs text-slate-500 dark:text-slate-400">{user?.email}</small>
      {syncStatus === 'syncing' ? <small className="block text-xs text-slate-500 dark:text-slate-400">Syncing account</small> : null}
      {syncStatus === 'synced' ? <small className="block text-xs text-green-700">Account synced</small> : null}
      {syncStatus === 'failed' ? <small className="block text-xs text-red-700">Account sync failed</small> : null}
      <div className="mt-1 flex flex-wrap gap-2">
        <LogoutButton />
      </div>
    </div>
  );
}
