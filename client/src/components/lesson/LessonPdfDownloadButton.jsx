import { useState } from 'react';
import { useApiClient } from '../../hooks/useApiClient.js';
import Button from '../ui/Button.jsx';
import ErrorMessage from '../ui/ErrorMessage.jsx';

export default function LessonPdfDownloadButton({ courseId, moduleIndex, lessonIndex, lessonTitle }) {
  const apiClient = useApiClient();
  const [isDownloading, setDownloading] = useState(false);
  const [error, setError] = useState('');

  async function handleDownload() {
    setDownloading(true);
    setError('');

    try {
      const pdfBlob = await apiClient(
        `/api/courses/${courseId}/module/${moduleIndex}/lesson/${lessonIndex}/pdf`,
        {
          headers: {
            Accept: 'application/pdf',
          },
          responseType: 'blob',
        },
      );

      const downloadUrl = URL.createObjectURL(pdfBlob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.download = `${toSafeFileName(lessonTitle)}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(downloadUrl);
    } catch (downloadError) {
      setError(downloadError.message);
    } finally {
      setDownloading(false);
    }
  }

  return (
    <section className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900" aria-labelledby="lesson-download-title">
      <div className="flex flex-col items-start justify-between gap-6 sm:flex-row sm:items-center">
        <div className="flex flex-col gap-1">
          <p className="text-xs font-bold uppercase tracking-widest text-blue-600">Export</p>
          <h2 className="text-xl font-semibold text-slate-900 dark:text-white" id="lesson-download-title">
            Download lesson
          </h2>
          <p className="text-sm leading-6 text-slate-500 dark:text-slate-400">
            Save this lesson as a styled PDF with the explanation, code blocks, and questions.
          </p>
        </div>
        <Button onClick={handleDownload} type="button" disabled={isDownloading}>
          {isDownloading ? 'Preparing PDF...' : 'Download PDF'}
        </Button>
      </div>
      {error && <ErrorMessage message={error} title="Could not download PDF" />}
    </section>
  );
}

function toSafeFileName(value) {
  return (value || 'lesson')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 80) || 'lesson';
}
