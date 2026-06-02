package com.texttolearn.video.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.texttolearn.video.config.YouTubeProperties;
import com.texttolearn.video.dto.YouTubeVideoResponse;
import com.texttolearn.video.dto.YouTubeVideoSearchResponse;
import com.texttolearn.video.error.VideoLookupException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class YouTubeVideoService {

    private static final String YOUTUBE_EMBED_BASE_URL = "https://www.youtube.com/embed/";
    private static final String YOUTUBE_WATCH_BASE_URL = "https://www.youtube.com/watch?v=";
    private static final Duration MINIMUM_REGULAR_VIDEO_DURATION = Duration.ofMinutes(4);

    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final YouTubeProperties youtubeProperties;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Autowired
    public YouTubeVideoService(ObjectMapper objectMapper, YouTubeProperties youtubeProperties) {
        this(objectMapper, youtubeProperties, Clock.systemUTC(), RestClient.builder());
    }

    YouTubeVideoService(
            ObjectMapper objectMapper,
            YouTubeProperties youtubeProperties,
            Clock clock,
            RestClient.Builder restClientBuilder
    ) {
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.youtubeProperties = youtubeProperties;
        this.restClient = restClientBuilder
                .baseUrl(youtubeProperties.baseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public YouTubeVideoSearchResponse searchEducationalVideos(String query) {
        return searchEducationalVideos(query, youtubeProperties.sanitizedMaxResults());
    }

    public YouTubeVideoSearchResponse searchEducationalVideos(String query, Integer maxResults) {
        int sanitizedMaxResults = maxResults == null
                ? youtubeProperties.sanitizedMaxResults()
                : youtubeProperties.sanitizedMaxResults(maxResults);
        String normalizedQuery = normalizeQuery(query);
        CacheEntry cachedEntry = cache.get(cacheKey(normalizedQuery, sanitizedMaxResults));
        if (cachedEntry != null && !cachedEntry.isExpired(clock.instant())) {
            return cachedEntry.response();
        }

        if (!youtubeProperties.isConfigured()) {
            throw new VideoLookupException("YouTube API key is not configured. Set YOUTUBE_API_KEY.");
        }

        try {
            String responseBody = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("part", "snippet")
                            .queryParam("q", normalizedQuery)
                            .queryParam("maxResults", searchCandidateCount(sanitizedMaxResults))
                            .queryParam("type", "video")
                            .queryParam("videoEmbeddable", "true")
                            .queryParam("videoDuration", "medium")
                            .queryParam("safeSearch", "moderate")
                            .queryParam(
                                    "fields",
                                    "items(id/videoId)"
                            )
                            .queryParam("key", youtubeProperties.apiKey())
                            .build())
                    .retrieve()
                    .body(String.class);

            List<String> videoIds = parseVideoIds(responseBody);
            if (videoIds.isEmpty()) {
                YouTubeVideoSearchResponse response = new YouTubeVideoSearchResponse(normalizedQuery, List.of());
                cache.put(cacheKey(normalizedQuery, sanitizedMaxResults), new CacheEntry(response, expiresAt()));
                return response;
            }

            String detailsResponseBody = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/videos")
                            .queryParam("part", "snippet,contentDetails,status")
                            .queryParam("id", String.join(",", videoIds))
                            .queryParam(
                                    "fields",
                                    "items(id,snippet/title,snippet/channelTitle,"
                                            + "snippet/thumbnails/default/url,snippet/thumbnails/medium/url,"
                                            + "contentDetails/duration,status/embeddable)"
                            )
                            .queryParam("key", youtubeProperties.apiKey())
                            .build())
                    .retrieve()
                    .body(String.class);

            YouTubeVideoSearchResponse response = new YouTubeVideoSearchResponse(
                    normalizedQuery,
                    parseVideos(detailsResponseBody).stream()
                            .limit(sanitizedMaxResults)
                            .toList()
            );
            cache.put(cacheKey(normalizedQuery, sanitizedMaxResults), new CacheEntry(response, expiresAt()));
            return response;
        } catch (RestClientResponseException exception) {
            throw new VideoLookupException(
                    "YouTube request failed with status " + exception.getStatusCode().value() + ".",
                    exception
            );
        } catch (RestClientException exception) {
            throw new VideoLookupException("Failed to fetch YouTube videos.", exception);
        }
    }

    List<YouTubeVideoResponse> parseVideos(String responseBody) {
        JsonNode root = readResponse(responseBody);
        JsonNode items = root.path("items");
        if (!items.isArray() || items.isEmpty()) {
            return List.of();
        }

        List<YouTubeVideoResponse> videos = new ArrayList<>();
        for (JsonNode item : items) {
            if (isShortOrNonEmbeddable(item)) {
                continue;
            }

            String videoId = firstNonBlank(textAt(item, "id"), textAt(item, "id", "videoId"));
            if (videoId == null || videoId.isBlank()) {
                continue;
            }

            JsonNode snippet = item.path("snippet");
            videos.add(new YouTubeVideoResponse(
                    videoId,
                    YOUTUBE_EMBED_BASE_URL + videoId,
                    YOUTUBE_WATCH_BASE_URL + videoId,
                    textAt(snippet, "title"),
                    textAt(snippet, "channelTitle"),
                    thumbnailUrl(snippet)
            ));
        }

        return videos;
    }

    List<String> parseVideoIds(String responseBody) {
        JsonNode root = readResponse(responseBody);
        JsonNode items = root.path("items");
        if (!items.isArray() || items.isEmpty()) {
            return List.of();
        }

        Set<String> videoIds = new LinkedHashSet<>();
        for (JsonNode item : items) {
            String videoId = firstNonBlank(textAt(item, "id", "videoId"), textAt(item, "id"));
            if (videoId != null) {
                videoIds.add(videoId);
            }
        }

        return List.copyOf(videoIds);
    }

    private JsonNode readResponse(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException exception) {
            throw new VideoLookupException("YouTube returned an unreadable response.", exception);
        }
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }

        String normalizedQuery = query.trim();
        if (normalizedQuery.length() > 180) {
            return normalizedQuery.substring(0, 180).trim();
        }

        return normalizedQuery;
    }

    private String cacheKey(String query, int maxResults) {
        return query.toLowerCase(Locale.ROOT) + "::" + maxResults;
    }

    private int searchCandidateCount(int requestedMaxResults) {
        return Math.max(requestedMaxResults * 4, 8);
    }

    private Instant expiresAt() {
        return clock.instant().plus(Duration.ofMinutes(youtubeProperties.sanitizedCacheTtlMinutes()));
    }

    private String thumbnailUrl(JsonNode snippet) {
        String medium = textAt(snippet, "thumbnails", "medium", "url");
        if (medium != null) {
            return medium;
        }

        return textAt(snippet, "thumbnails", "default", "url");
    }

    private String textAt(JsonNode node, String... path) {
        JsonNode current = node;
        for (String segment : path) {
            current = current.path(segment);
        }

        if (current.isMissingNode() || current.isNull()) {
            return null;
        }

        return current.asText();
    }

    private boolean isShortOrNonEmbeddable(JsonNode item) {
        JsonNode embeddable = item.path("status").path("embeddable");
        if (!embeddable.isMissingNode() && !embeddable.asBoolean(false)) {
            return true;
        }

        String durationText = textAt(item, "contentDetails", "duration");
        if (durationText == null || durationText.isBlank()) {
            return false;
        }

        try {
            return Duration.parse(durationText).compareTo(MINIMUM_REGULAR_VIDEO_DURATION) < 0;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }

        if (second != null && !second.isBlank()) {
            return second;
        }

        return null;
    }

    private record CacheEntry(YouTubeVideoSearchResponse response, Instant expiresAt) {

        boolean isExpired(Instant now) {
            return !expiresAt.isAfter(now);
        }
    }
}
