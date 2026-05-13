export default function LoadingSpinner({ label = 'Loading' }) {
  return (
    <div
      className="inline-flex items-center gap-3 rounded-lg border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-600 shadow-sm dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300"
      role="status"
      aria-live="polite"
    >
      <span
        className="h-5 w-5 animate-spin rounded-full border-2 border-blue-200 border-t-blue-600 dark:border-blue-950 dark:border-t-blue-400"
        aria-hidden="true"
      />
      <span>{label}</span>
    </div>
  );
}
