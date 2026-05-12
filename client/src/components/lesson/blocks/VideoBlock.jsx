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
    <section className="lesson-block video-block">
      <h3>{block.title || 'Related videos'}</h3>
      {visibleVideos.length > 0 ? (
        <div className="video-grid">
          {visibleVideos.map((video) => (
            <article className="video-result" key={video.videoId || video.embedUrl}>
              <div className="video-frame">
                <iframe
                  title={video.title || block.title || 'Lesson video'}
                  src={video.embedUrl}
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                  allowFullScreen
                />
              </div>
              <div className="video-result-meta">
                <strong>{video.title || 'Related video'}</strong>
                {video.channelTitle && <span>{video.channelTitle}</span>}
              </div>
            </article>
          ))}
        </div>
      ) : isLoading ? (
        <div className="video-pending">
          <p>Finding a relevant video for this lesson.</p>
        </div>
      ) : searchUrl ? (
        <div className="video-pending">
          <p>
            {error
              ? 'Could not embed a video right now.'
              : 'No embeddable video was found for this query yet.'}
          </p>
          <a className="video-search-link" href={searchUrl} target="_blank" rel="noreferrer">
            Preview search results
          </a>
        </div>
      ) : (
        <p className="muted-text">A video will be added here when one is available.</p>
      )}
    </section>
  );
}
