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
    <section className="flex flex-col gap-5 rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900" aria-labelledby="lesson-audio-title">
      <div>
        <p className="mb-2 text-xs font-bold uppercase tracking-widest text-blue-600">Audio</p>
        <h2 className="text-2xl font-bold text-slate-900 dark:text-white" id="lesson-audio-title">
          {selectedLanguageLabel(language)} explanation
        </h2>
        <p className="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">
          Listen to a simplified audio walkthrough of this lesson, generated from the lesson
          content.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <label className="flex flex-col gap-2 text-sm font-semibold text-slate-700 dark:text-slate-300">
          <span>Explanation language</span>
          <select
            className="rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-60 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-200"
            value={language}
            onChange={handleLanguageChange}
            disabled={isLoading}
          >
            {AUDIO_LANGUAGES.map((option) => (
              <option key={option.code} value={option.code}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label className="flex flex-col gap-2 text-sm font-semibold text-slate-700 dark:text-slate-300">
          <span>Voice</span>
          <select
            className="rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-60 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-200"
            value={voiceName}
            onChange={handleVoiceChange}
            disabled={isLoading}
          >
            {AUDIO_VOICES.map((voice) => (
              <option key={voice.name} value={voice.name}>
                {voice.label} - {voice.description}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className="flex flex-wrap gap-3">
        <Button type="button" onClick={handlePrepareAudio} disabled={isLoading}>
          {isLoading ? 'Preparing audio' : audioUrl ? 'Prepare again' : `Prepare ${selectedLanguageLabel(language)} audio`}
        </Button>
        {audioUrl && (
          <a
            className="inline-flex items-center justify-center rounded-lg border border-slate-300 bg-white px-5 py-2.5 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-200 dark:hover:bg-slate-800"
            href={audioUrl}
            download={downloadName(lessonTitle, language, voiceName)}
          >
            Download audio
          </a>
        )}
      </div>

      {audioUrl && (
        <audio className="mt-1 w-full" controls src={audioUrl}>
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
