import { Link } from 'react-router-dom';

export default function EmptyState({ heading, body, linkTo, linkLabel }) {
  return (
    <div className="flex flex-col items-center gap-4 rounded-xl border border-slate-200 bg-white p-10 text-center dark:border-slate-800 dark:bg-slate-900">
      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-blue-50 text-lg font-bold text-blue-600 dark:bg-blue-950 dark:text-blue-300">
        T
      </div>
      <h2 className="text-xl font-semibold text-slate-900 dark:text-white">{heading}</h2>
      {body ? <p className="max-w-md text-sm leading-6 text-slate-500 dark:text-slate-400">{body}</p> : null}
      {linkLabel && linkTo ? (
        <Link
          className="inline-flex items-center justify-center rounded-lg bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          to={linkTo}
        >
          {linkLabel}
        </Link>
      ) : null}
    </div>
  );
}
