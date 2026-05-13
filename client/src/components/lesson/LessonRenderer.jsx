import CodeBlock from './blocks/CodeBlock.jsx';
import HeadingBlock from './blocks/HeadingBlock.jsx';
import MCQBlock from './blocks/MCQBlock.jsx';
import ParagraphBlock from './blocks/ParagraphBlock.jsx';
import VideoBlock from './blocks/VideoBlock.jsx';

const blockComponents = {
  heading: HeadingBlock,
  paragraph: ParagraphBlock,
  code: CodeBlock,
  video: VideoBlock,
  mcq: MCQBlock,
};

export default function LessonRenderer({ content = [] }) {
  if (!Array.isArray(content) || content.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-slate-300 bg-white p-8 text-center dark:border-slate-700 dark:bg-slate-900">
        <h2 className="text-xl font-semibold text-slate-900 dark:text-white">Lesson content is not available yet</h2>
        <p className="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">
          This lesson will appear here after the course content is generated.
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-5">
      {content.map((block, index) => {
        const BlockComponent = blockComponents[block?.type];

        if (!BlockComponent) {
          return (
            <div
              className="rounded-xl border border-amber-200 bg-amber-50 p-5 text-amber-900"
              key={`unsupported-${index}`}
            >
              <strong className="block text-sm font-semibold">Unsupported content block</strong>
              <p className="mt-1 text-sm">Type: {block?.type || 'unknown'}</p>
            </div>
          );
        }

        return <BlockComponent block={block} key={`${block.type}-${index}`} />;
      })}
    </div>
  );
}
