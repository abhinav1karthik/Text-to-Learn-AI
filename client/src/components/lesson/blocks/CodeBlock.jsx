export default function CodeBlock({ block }) {
  return (
    <section className="lesson-block code-block">
      <div className="code-block-header">
        <span>{block.language || 'text'}</span>
      </div>
      <pre>
        <code>{block.text}</code>
      </pre>
    </section>
  );
}
