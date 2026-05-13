export default function CodeBlock({ block }) {
  const codeText = block.text || block.code || block.source || block.sourceCode || block.content || '';

  return (
    <section className="overflow-hidden rounded-xl border border-slate-200 dark:border-slate-800">
      <div className="flex justify-end border-b border-slate-200 bg-slate-100 px-4 py-2 dark:border-slate-800 dark:bg-slate-900">
        <span className="text-xs font-bold uppercase tracking-wide text-slate-500 dark:text-slate-400">
          {block.language || 'text'}
        </span>
      </div>
      <pre className="m-0 overflow-x-auto bg-gray-900 p-5 text-gray-100">
        <code className="whitespace-pre font-mono text-sm leading-relaxed">
          {codeText || 'Code example was not available for this block.'}
        </code>
      </pre>
    </section>
  );
}
