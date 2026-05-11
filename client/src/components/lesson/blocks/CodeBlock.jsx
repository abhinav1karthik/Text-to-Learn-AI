export default function CodeBlock({ block }) {
  const codeText = block.text || block.code || block.source || block.sourceCode || block.content || '';

  return (
    <section className="lesson-block code-block">
      <div className="code-block-header">
        <span>{block.language || 'text'}</span>
      </div>
      <pre>
        <code>{codeText || 'Code example was not available for this block.'}</code>
      </pre>
    </section>
  );
}
