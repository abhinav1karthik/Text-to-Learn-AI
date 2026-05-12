import { useEffect, useState } from 'react';
import Button from '../ui/Button.jsx';
import ErrorMessage from '../ui/ErrorMessage.jsx';
import { useApiClient } from '../../hooks/useApiClient.js';
import { AUDIO_LANGUAGES } from '../../utils/audioLanguages.js';
import { AUDIO_VOICES } from '../../utils/audioVoices.js';

export default function LessonAudioPlayer({ courseId, moduleIndex, lessonIndex, lessonTitle }) {
  const apiClient = useApiClient();
  const [audioUrl, setAudioUrl] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setLoading] = useState(false);
  const [language, setLanguage] = useState(AUDIO_LANGUAGES[0].code);
  const [voiceName, setVoiceName] = useState(AUDIO_VOICES[0].name);

  useEffect(() => {
    return () => {
      if (audioUrl) {
        URL.revokeObjectURL(audioUrl);
      }
    };
  }, [audioUrl]);

  function handleLanguageChange(event) {
    setLanguage(event.target.value);
    resetPreparedAudio();
  }

  function handleVoiceChange(event) {
    setVoiceName(event.target.value);
    resetPreparedAudio();
  }

  function resetPreparedAudio() {
    setError('');
    if (audioUrl) {
      URL.revokeObjectURL(audioUrl);
      setAudioUrl('');
    }
  }

  async function handlePrepareAudio() {
    setLoading(true);
    setError('');

    try {
      const searchParams = new URLSearchParams({
        language,
        voiceName,
      });
      const audioBlob = await apiClient(
        `/api/courses/${courseId}/module/${moduleIndex}/lesson/${lessonIndex}/audio?${searchParams}`,
        {
          responseType: 'blob',
          headers: {
            Accept: 'audio/wav, application/json',
          },
        },
      );
      const nextAudioUrl = URL.createObjectURL(audioBlob);
      if (audioUrl) {
        URL.revokeObjectURL(audioUrl);
      }
      setAudioUrl(nextAudioUrl);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="lesson-audio-panel" aria-labelledby="lesson-audio-title">
      <div>
        <p className="eyebrow">Audio</p>
        <h2 id="lesson-audio-title">{selectedLanguageLabel(language)} explanation</h2>
        <p>
          Listen to a simplified audio walkthrough of this lesson, generated from the lesson
          content.
        </p>
      </div>

      <div className="audio-options-grid">
        <label className="audio-option-field">
          <span>Explanation language</span>
          <select value={language} onChange={handleLanguageChange} disabled={isLoading}>
            {AUDIO_LANGUAGES.map((option) => (
              <option key={option.code} value={option.code}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label className="audio-option-field">
          <span>Voice</span>
          <select value={voiceName} onChange={handleVoiceChange} disabled={isLoading}>
            {AUDIO_VOICES.map((voice) => (
              <option key={voice.name} value={voice.name}>
                {voice.label} - {voice.description}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className="lesson-audio-actions">
        <Button type="button" onClick={handlePrepareAudio} disabled={isLoading}>
          {isLoading ? 'Preparing audio' : audioUrl ? 'Prepare again' : `Prepare ${selectedLanguageLabel(language)} audio`}
        </Button>
        {audioUrl && (
          <a
            className="button button-secondary button-link"
            href={audioUrl}
            download={downloadName(lessonTitle, language, voiceName)}
          >
            Download audio
          </a>
        )}
      </div>

      {audioUrl && (
        <audio className="lesson-audio-player" controls src={audioUrl}>
          Your browser does not support audio playback.
        </audio>
      )}

      <ErrorMessage title="Could not prepare audio" message={error} />
    </section>
  );
}

function selectedLanguageLabel(language) {
  return AUDIO_LANGUAGES.find((option) => option.code === language)?.label ?? 'Hinglish';
}

function downloadName(lessonTitle, language, voiceName) {
  const normalizedTitle = lessonTitle
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
  const normalizedVoice = voiceName
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');

  return `${normalizedTitle || 'lesson'}-${language}-${normalizedVoice || 'voice'}.wav`;
}
