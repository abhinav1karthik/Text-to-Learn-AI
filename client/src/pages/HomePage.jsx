import Button from '../components/ui/Button.jsx';
import TextInput from '../components/ui/TextInput.jsx';

export default function HomePage() {
  return (
    <section className="page-stack">
      <div>
        <p className="eyebrow">Course builder</p>
        <h1>Generate a course from any topic</h1>
        <p className="lead">
          Turn a topic into a structured learning path with modules, lessons, and guided
          explanations.
        </p>
      </div>

      <form className="prompt-panel">
        <div className="prompt-row">
          <TextInput
            id="topic"
            label="Topic prompt"
            type="text"
            placeholder="Segment Trees and Its Applications"
            disabled
          />
          <Button type="button" disabled>
            Generate
          </Button>
        </div>
        <p>
          Enter a topic such as data structures, guitar basics, or driving skills to create
          a personalized course outline.
        </p>
      </form>
    </section>
  );
}
