import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <section className="flex max-w-[720px] flex-col gap-7">
      <div>
        <p className="mb-2 text-xs font-bold uppercase tracking-widest text-blue-600">404</p>
        <h1 className="mb-3 text-4xl font-bold leading-tight text-slate-900 dark:text-white">Page not found</h1>
        <p className="max-w-2xl text-lg leading-relaxed text-slate-500 dark:text-slate-400">
          The route you opened does not exist in the app shell yet.
        </p>
      </div>
      <Link
        className="inline-flex w-fit items-center justify-center rounded-lg bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
        to="/"
      >
        Go home
      </Link>
    </section>
  );
}
