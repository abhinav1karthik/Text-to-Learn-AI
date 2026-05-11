package com.texttolearn.video.dto;

import java.util.List;

public record YouTubeVideoSearchResponse(
        String query,
        List<YouTubeVideoResponse> videos
) {
}
