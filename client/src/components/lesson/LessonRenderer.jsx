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
      <div className="lesson-empty">
        <h2>Lesson content is not available yet</h2>
        <p>This lesson will appear here after the course content is generated.</p>
      </div>
    );
  }

  return (
    <div className="lesson-renderer">
      {content.map((block, index) => {
        const BlockComponent = blockComponents[block?.type];

        if (!BlockComponent) {
          return (
            <div className="lesson-block unsupported-block" key={`unsupported-${index}`}>
              <strong>Unsupported content block</strong>
              <p>Type: {block?.type || 'unknown'}</p>
            </div>
          );
        }

        return <BlockComponent block={block} key={`${block.type}-${index}`} />;
      })}
    </div>
  );
}
