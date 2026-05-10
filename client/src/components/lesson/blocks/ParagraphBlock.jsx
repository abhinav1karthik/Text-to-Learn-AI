export default function ParagraphBlock({ block }) {
  return (
    <section className="lesson-block paragraph-block">
      <p>{block.text}</p>
    </section>
  );
}
