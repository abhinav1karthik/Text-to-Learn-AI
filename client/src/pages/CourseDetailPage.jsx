import { Link, useParams } from 'react-router-dom';
import { ROUTES } from '../utils/routes.js';

export default function CourseDetailPage() {
  const { courseId } = useParams();

  return (
    <section className="page-stack">
      <div>
        <p className="eyebrow">Course</p>
        <h1>Course outline</h1>
        <p className="lead">
          Course <code>{courseId}</code> is shown as a sample outline with modules and
          planned lessons.
        </p>
      </div>

      <div className="outline-list">
        <article>
          <span>Module 1</span>
          <h2>Foundations</h2>
          <Link to={ROUTES.courseLesson(courseId, 0, 0)}>Open sample lesson</Link>
        </article>
      </div>
    </section>
  );
}
