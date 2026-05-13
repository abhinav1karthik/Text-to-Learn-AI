import LoginButton from '../components/auth/LoginButton.jsx';
import { useAuth } from '../hooks/useAuth.js';

export default function LoginPage() {
  const { isConfigured } = useAuth();

  return (
    <section className="flex max-w-[720px] flex-col gap-7">
      <div>
        <p className="mb-2 text-xs font-bold uppercase tracking-widest text-blue-600">Account</p>
        <h1 className="mb-3 text-4xl font-bold leading-tight text-slate-900 dark:text-white">Log in to Text To Learn</h1>
        <p className="max-w-2xl text-lg leading-relaxed text-slate-500 dark:text-slate-400">
          Access saved courses, lesson progress, and generated learning materials from your
          account.
        </p>
      </div>

      <div className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
        <LoginButton />
        {!isConfigured ? (
          <p className="text-sm leading-6 text-slate-500 dark:text-slate-400">
            Auth0 settings are missing. Add the required values in
            <code className="mx-1 rounded bg-slate-100 px-1.5 py-0.5 text-xs text-slate-700">client/.env.local</code>
            to enable login.
          </p>
        ) : null}
      </div>
    </section>
  );
}
