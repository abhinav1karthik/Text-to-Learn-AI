import { useState } from 'react';

export default function MCQBlock({ block }) {
  const [selectedOption, setSelectedOption] = useState(null);
  const options = Array.isArray(block.options) ? block.options : [];
  const hasAnswered = selectedOption !== null;
  const isCorrect = hasAnswered && selectedOption === block.answer;

  return (
    <section className="lesson-block mcq-block">
      <h3>{block.question}</h3>

      <div className="mcq-options">
        {options.map((option, index) => {
          const selected = selectedOption === index;
          const correct = hasAnswered && index === block.answer;
          const incorrect = selected && !correct;

          return (
            <button
              className={[
                'mcq-option',
                selected ? 'is-selected' : '',
                correct ? 'is-correct' : '',
                incorrect ? 'is-incorrect' : '',
              ]
                .filter(Boolean)
                .join(' ')}
              key={`${index}-${option}`}
              onClick={() => setSelectedOption(index)}
              type="button"
            >
              <span>{String.fromCharCode(65 + index)}</span>
              {option}
            </button>
          );
        })}
      </div>

      {hasAnswered && (
        <p className={isCorrect ? 'mcq-feedback correct' : 'mcq-feedback incorrect'}>
          {isCorrect ? 'Correct answer.' : 'Not quite. Try reviewing the explanation above.'}
        </p>
      )}
    </section>
  );
}
