package com.texttolearn.video.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.texttolearn.video.config.YouTubeProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class YouTubeVideoServiceTests {

    private final YouTubeVideoService service = new YouTubeVideoService(
            new ObjectMapper(),
            new YouTubeProperties("test-key", "https://www.googleapis.com/youtube/v3", 3, 1_440),
            Clock.fixed(Instant.parse("2026-05-11T00:00:00Z"), ZoneOffset.UTC),
            RestClient.builder()
    );

    @Test
    void parsesSearchResultsIntoEmbeddableVideoResponses() {
        String responseBody = """
                {
                  "items": [
                    {
                      "id": { "videoId": "abc123" },
                      "snippet": {
                        "title": "Segment Trees Explained",
                        "channelTitle": "Algorithms Channel",
                        "thumbnails": {
                          "medium": { "url": "https://img.youtube.com/abc123.jpg" }
                        }
                      }
                    }
                  ]
                }
                """;

        var videos = service.parseVideos(responseBody);

        assertThat(videos).hasSize(1);
        assertThat(videos.getFirst().videoId()).isEqualTo("abc123");
        assertThat(videos.getFirst().embedUrl()).isEqualTo("https://www.youtube.com/embed/abc123");
        assertThat(videos.getFirst().watchUrl()).isEqualTo("https://www.youtube.com/watch?v=abc123");
        assertThat(videos.getFirst().title()).isEqualTo("Segment Trees Explained");
        assertThat(videos.getFirst().channelTitle()).isEqualTo("Algorithms Channel");
        assertThat(videos.getFirst().thumbnailUrl()).isEqualTo("https://img.youtube.com/abc123.jpg");
    }

    @Test
    void rejectsBlankSearchQueries() {
        assertThatThrownBy(() -> service.searchEducationalVideos("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("query must not be blank");
    }

    @Test
    void clampsRequestedVideoResultCount() {
        YouTubeProperties properties = new YouTubeProperties(
                "test-key",
                "https://www.googleapis.com/youtube/v3",
                3,
                1_440
        );

        assertThat(properties.sanitizedMaxResults(1)).isEqualTo(1);
        assertThat(properties.sanitizedMaxResults(9)).isEqualTo(3);
        assertThat(properties.sanitizedMaxResults(0)).isEqualTo(3);
    }
}
