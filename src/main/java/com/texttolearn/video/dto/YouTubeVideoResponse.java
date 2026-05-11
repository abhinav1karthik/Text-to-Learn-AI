package com.texttolearn.video.dto;

public record YouTubeVideoResponse(
        String videoId,
        String embedUrl,
        String watchUrl,
        String title,
        String channelTitle,
        String thumbnailUrl
) {
}
