export default function HeadingBlock({ block }) {
  return (
    <section className="lesson-block heading-block">
      <h2>{block.text}</h2>
    </section>
  );
}
