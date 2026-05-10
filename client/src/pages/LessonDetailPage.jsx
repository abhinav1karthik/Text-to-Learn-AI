import { useParams } from 'react-router-dom';
import LessonRenderer from '../components/lesson/LessonRenderer.jsx';

const sampleLessonContent = [
  {
    type: 'heading',
    text: 'Introduction to Segment Trees',
  },
  {
    type: 'paragraph',
    text: 'A segment tree is a data structure used to answer range queries efficiently while still allowing updates to the original array.',
  },
  {
    type: 'paragraph',
    text: 'The main idea is to divide the array into smaller intervals. Each node stores useful information about one interval, such as its sum, minimum, maximum, or greatest common divisor.',
  },
  {
    type: 'code',
    language: 'java',
    text: `int query(int node, int left, int right, int queryLeft, int queryRight) {
    if (queryRight < left || right < queryLeft) {
        return 0;
    }

    if (queryLeft <= left && right <= queryRight) {
        return tree[node];
    }

    int mid = (left + right) / 2;
    return query(node * 2, left, mid, queryLeft, queryRight)
        + query(node * 2 + 1, mid + 1, right, queryLeft, queryRight);
}`,
  },
  {
    type: 'video',
    title: 'Segment tree walkthrough',
    url: 'https://www.youtube.com/watch?v=ZBHKZF5w4YU',
  },
  {
    type: 'mcq',
    question: 'What is the main benefit of a segment tree?',
    options: [
      'It stores only sorted arrays',
      'It answers range queries and updates efficiently',
      'It replaces every graph algorithm',
    ],
    answer: 1,
  },
];

export default function LessonDetailPage() {
  const { courseId, lessonId, lessonIndex, moduleIndex } = useParams();
  const routeLabel =
    courseId && moduleIndex && lessonIndex
      ? `${courseId} / module ${Number(moduleIndex) + 1} / lesson ${Number(lessonIndex) + 1}`
      : lessonId;

  return (
    <section className="page-stack">
      <div>
        <p className="eyebrow">Lesson</p>
        <h1>Sample lesson</h1>
        <p className="lead">
          Lesson <code>{routeLabel}</code> is rendered from structured JSON blocks.
        </p>
      </div>

      <LessonRenderer content={sampleLessonContent} />
    </section>
  );
}
