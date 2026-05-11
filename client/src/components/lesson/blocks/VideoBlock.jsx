function getYouTubeEmbedUrl(block) {
  if (block.embedUrl) {
    return block.embedUrl;
  }

  if (block.videoId) {
    return `https://www.youtube.com/embed/${block.videoId}`;
  }

  if (!block.url) {
    return null;
  }

  try {
    const parsedUrl = new URL(block.url);

    if (parsedUrl.hostname.includes('youtu.be')) {
      return `https://www.youtube.com/embed/${parsedUrl.pathname.slice(1)}`;
    }

    if (parsedUrl.hostname.includes('youtube.com')) {
      const videoId = parsedUrl.searchParams.get('v');
      return videoId ? `https://www.youtube.com/embed/${videoId}` : url;
    }

    return block.url;
  } catch {
    return null;
  }
}

export default function VideoBlock({ block }) {
  const embedUrl = getYouTubeEmbedUrl(block);
  const searchUrl = block.query
    ? `https://www.youtube.com/results?search_query=${encodeURIComponent(block.query)}`
    : null;

  return (
    <section className="lesson-block video-block">
      <h3>{block.title || 'Related video'}</h3>
      {embedUrl ? (
        <div className="video-frame">
          <iframe
            title={block.title || 'Lesson video'}
            src={embedUrl}
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            allowFullScreen
          />
        </div>
      ) : searchUrl ? (
        <div className="video-pending">
          <p>Video will be embedded after we select the best match for this query.</p>
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
