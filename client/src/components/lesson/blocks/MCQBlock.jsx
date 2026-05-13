import { useState } from 'react';

export default function MCQBlock({ block }) {
  const [selectedOption, setSelectedOption] = useState(null);
  const options = Array.isArray(block.options) ? block.options : [];
  const hasAnswered = selectedOption !== null;
  const isCorrect = hasAnswered && selectedOption === block.answer;

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
      <h3 className="m-0 mb-4 text-base font-semibold text-slate-900 dark:text-white">{block.question}</h3>

      <div className="flex flex-col gap-2">
        {options.map((option, index) => {
          const selected = selectedOption === index;
          const correct = hasAnswered && index === block.answer;
          const incorrect = selected && !correct;

          return (
            <button
              className={[
                'flex w-full items-center gap-3 rounded-lg border border-slate-200 px-4 py-3 text-left text-sm text-slate-700 transition-colors hover:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-700 dark:text-slate-200 dark:hover:border-blue-600',
                selected ? 'border-blue-500 bg-blue-50 dark:bg-blue-950/40' : '',
                correct ? 'border-green-500 bg-green-50 text-green-800 dark:bg-green-950/40 dark:text-green-200' : '',
                incorrect ? 'border-red-400 bg-red-50 text-red-700 dark:bg-red-950/40 dark:text-red-200' : '',
              ]
                .filter(Boolean)
                .join(' ')}
              key={`${index}-${option}`}
              onClick={() => setSelectedOption(index)}
              type="button"
            >
              <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-blue-50 text-xs font-bold text-blue-700">
                {String.fromCharCode(65 + index)}
              </span>
              {option}
            </button>
          );
        })}
      </div>

      {hasAnswered && (
        <div>
          <p className={isCorrect ? 'mt-4 text-sm font-semibold text-green-700' : 'mt-4 text-sm font-semibold text-red-600'}>
            {isCorrect ? 'Correct answer.' : 'Not quite. Try reviewing the explanation.'}
          </p>
          {block.explanation ? <p className="mt-2 text-sm leading-relaxed text-slate-500 dark:text-slate-400">{block.explanation}</p> : null}
          <button
            className="mt-3 text-sm font-semibold text-blue-600 hover:underline focus:outline-none focus:ring-2 focus:ring-blue-500"
            onClick={() => setSelectedOption(null)}
            type="button"
          >
            Try again
          </button>
        </div>
      )}
    </section>
  );
}
