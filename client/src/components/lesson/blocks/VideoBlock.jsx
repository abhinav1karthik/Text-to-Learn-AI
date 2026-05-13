import { useEffect, useMemo, useState } from 'react';
import { useApiClient } from '../../../hooks/useApiClient.js';

function getYouTubeEmbedUrl(block) {
  if (block.embedUrl) {
    return block.embedUrl;
  }

  if (block.videoId) {
    return `https://www.youtube.com/embed/${block.videoId}`;
  }

  const sourceUrl = block.url || block.watchUrl;
  if (!sourceUrl) {
    return null;
  }

  try {
    const parsedUrl = new URL(sourceUrl);

    if (parsedUrl.hostname.includes('youtu.be')) {
      return `https://www.youtube.com/embed/${parsedUrl.pathname.slice(1)}`;
    }

    if (parsedUrl.hostname.includes('youtube.com')) {
      const videoId = parsedUrl.searchParams.get('v');
      return videoId ? `https://www.youtube.com/embed/${videoId}` : sourceUrl;
    }

    return sourceUrl;
  } catch {
    return null;
  }
}

function getSavedVideos(block) {
  if (!Array.isArray(block.videos)) {
    return [];
  }

  return block.videos
    .map((video) => ({
      ...video,
      embedUrl: getYouTubeEmbedUrl(video),
    }))
    .filter((video) => video.embedUrl);
}

function getVideoQuery(block) {
  return block.query || block.searchQuery || block.text || '';
}

function getMaxResults(block) {
  const maxResults = Number(block.maxResults);
  if (!Number.isFinite(maxResults) || maxResults <= 0) {
    return 3;
  }

  return Math.min(Math.trunc(maxResults), 3);
}

export default function VideoBlock({ block, query: queryProp }) {
  const apiClient = useApiClient();
  const [videos, setVideos] = useState([]);
  const [error, setError] = useState('');
  const [isLoading, setLoading] = useState(false);
  const savedVideos = useMemo(() => getSavedVideos(block), [block]);
  const directEmbedUrl = getYouTubeEmbedUrl(block);
  const query = queryProp || getVideoQuery(block);
  const maxResults = getMaxResults(block);
  const searchUrl = query
    ? `https://www.youtube.com/results?search_query=${encodeURIComponent(query)}`
    : null;
  const directVideo = directEmbedUrl
    ? [
        {
          embedUrl: directEmbedUrl,
          title: block.title || 'Related video',
          channelTitle: block.channelTitle,
        },
      ]
    : [];
  const visibleVideos =
    directVideo.length > 0 ? directVideo : savedVideos.length > 0 ? savedVideos : videos;

  useEffect(() => {
    if (directEmbedUrl || savedVideos.length > 0 || !query) {
      setVideos([]);
      setError('');
      setLoading(false);
      return undefined;
    }

    let cancelled = false;

    async function loadVideo() {
      setLoading(true);
      setError('');
      setVideos([]);

      try {
        const response = await apiClient(
          `/api/youtube?query=${encodeURIComponent(query)}&maxResults=${maxResults}`,
        );
        if (!cancelled) {
          setVideos(response.videos ?? []);
        }
      } catch (requestError) {
        if (!cancelled) {
          setError(requestError.message);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadVideo();

    return () => {
      cancelled = true;
    };
  }, [apiClient, directEmbedUrl, maxResults, query, savedVideos.length]);

  return (
    <section className="overflow-hidden rounded-xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900">
      <h3 className="m-0 px-5 pb-3 pt-5 text-base font-semibold text-slate-900 dark:text-white">
        {block.title || 'Related videos'}
      </h3>
      {visibleVideos.length > 0 ? (
        <div className="flex flex-col gap-4 px-5 pb-5">
          {visibleVideos.map((video) => (
            <article
              className="overflow-hidden rounded-xl border border-slate-100 bg-slate-50 dark:border-slate-800 dark:bg-slate-950"
              key={video.videoId || video.embedUrl}
            >
              <div className="relative aspect-video bg-gray-900">
                <iframe
                  title={video.title || block.title || 'Lesson video'}
                  className="absolute inset-0 h-full w-full border-0"
                  src={video.embedUrl}
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                  allowFullScreen
                />
              </div>
              <div className="flex flex-col gap-1 px-4 py-3">
                <strong className="text-sm font-semibold text-slate-900 dark:text-white">{video.title || 'Related video'}</strong>
                {video.channelTitle && <span className="text-xs font-medium text-slate-500 dark:text-slate-400">{video.channelTitle}</span>}
              </div>
            </article>
          ))}
        </div>
      ) : isLoading ? (
        <div className="flex flex-col gap-3 px-5 pb-5">
          <p className="text-sm leading-6 text-slate-500 dark:text-slate-400">Finding a relevant video for this lesson.</p>
        </div>
      ) : searchUrl ? (
        <div className="flex flex-col gap-3 px-5 pb-5">
          <p className="text-sm leading-6 text-slate-500 dark:text-slate-400">
            {error
              ? 'Could not embed a video right now.'
              : 'No embeddable video was found for this query yet.'}
          </p>
          <a
            className="inline-flex rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-semibold text-blue-600 no-underline transition-colors hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-800 dark:bg-slate-950 dark:text-blue-400 dark:hover:bg-blue-950/30"
            href={searchUrl}
            target="_blank"
            rel="noreferrer"
          >
            Preview search results
          </a>
        </div>
      ) : (
        <p className="px-5 pb-5 text-sm leading-6 text-slate-500 dark:text-slate-400">A video will be added here when one is available.</p>
      )}
    </section>
  );
}
