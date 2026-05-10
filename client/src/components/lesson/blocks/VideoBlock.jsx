function getYouTubeEmbedUrl(url) {
  if (!url) {
    return null;
  }

  try {
    const parsedUrl = new URL(url);

    if (parsedUrl.hostname.includes('youtu.be')) {
      return `https://www.youtube.com/embed/${parsedUrl.pathname.slice(1)}`;
    }

    if (parsedUrl.hostname.includes('youtube.com')) {
      const videoId = parsedUrl.searchParams.get('v');
      return videoId ? `https://www.youtube.com/embed/${videoId}` : url;
    }

    return url;
  } catch {
    return null;
  }
}

export default function VideoBlock({ block }) {
  const embedUrl = getYouTubeEmbedUrl(block.url);

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
      ) : (
        <p className="muted-text">A video will be added here when one is available.</p>
      )}
    </section>
  );
}
