import { useParams } from 'react-router-dom';

export default function LessonDetailPage() {
  const { lessonId } = useParams();

  return (
    <section className="page-stack">
      <div>
        <p className="eyebrow">Lesson</p>
        <h1>Sample lesson</h1>
        <p className="lead">
          Lesson <code>{lessonId}</code> shows how a generated lesson can be presented with
          structured learning blocks.
        </p>
      </div>

      <div className="lesson-preview">
        <h2>Planned lesson state</h2>
        <p>
          Lessons start as <code>PLANNED</code>. When the user opens one, the backend will
          lazily generate content and change it to <code>GENERATED</code>.
        </p>
      </div>
    </section>
  );
}
