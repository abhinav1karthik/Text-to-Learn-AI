export default function ParagraphBlock({ block }) {
  return (
    <section className="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
      <p className="m-0 text-base leading-relaxed text-slate-600 dark:text-slate-300">{block.text}</p>
    </section>
  );
}
