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
    <section className="lesson-download-panel" aria-labelledby="lesson-download-title">
      <div>
        <p className="eyebrow">Export</p>
        <h2 id="lesson-download-title">Download lesson</h2>
        <p>Save this lesson as a styled PDF with the explanation, code blocks, and questions.</p>
      </div>
      <Button onClick={handleDownload} type="button" disabled={isDownloading}>
        {isDownloading ? 'Preparing PDF...' : 'Download PDF'}
      </Button>
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
