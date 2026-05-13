export default function HeadingBlock({ block }) {
  return (
    <section className="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
      <h2 className="m-0 text-2xl font-bold leading-snug text-slate-900 dark:text-white">{block.text}</h2>
    </section>
  );
}
