package com.texttolearn.pdf.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.texttolearn.course.dto.LessonResponse;
import com.texttolearn.course.model.LessonStatus;
import com.texttolearn.pdf.dto.LessonPdfResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LessonPdfServiceTests {

    private final LessonPdfService lessonPdfService = new LessonPdfService();

    @Test
    void generatesPdfBytesForStructuredLessonContent() {
        LessonPdfResponse response = lessonPdfService.generateLessonPdf(lessonResponse());

        assertThat(response.contentType()).isEqualTo("application/pdf");
        assertThat(response.fileName()).isEqualTo("range-sum-query.pdf");
        assertThat(new String(response.pdf(), 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(response.pdf().length).isGreaterThan(1_000);
    }

    @Test
    void rendersEscapedHtmlWithRichTextFormatting() {
        String html = lessonPdfService.renderLessonHtml(lessonResponse());

        assertThat(html).contains("Range Sum Query");
        assertThat(html).contains("<strong>important</strong>");
        assertThat(html).contains("Line one<br />Line two");
        assertThat(html).contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;");
        assertThat(html).contains("https://www.youtube.com/watch?v=abc123");
        assertThat(html).contains("class=\"mcq-option correct\"");
    }

    private LessonResponse lessonResponse() {
        return new LessonResponse(
                UUID.randomUUID(),
                "Range Sum Query",
                0,
                LessonStatus.GENERATED,
                List.of("Understand **important** range query ideas"),
                List.of(
                        Map.of("type", "heading", "text", "Segment Tree Basics"),
                        Map.of("type", "paragraph", "text", "Line one\nLine two"),
                        Map.of("type", "paragraph", "text", "<script>alert('x')</script>"),
                        Map.of("type", "code", "language", "java", "text", "class SegmentTree {}"),
                        Map.of(
                                "type", "video",
                                "query", "segment tree tutorial",
                                "videos", List.of(Map.of(
                                        "videoId", "abc123",
                                        "title", "Segment Tree Tutorial",
                                        "watchUrl", "https://www.youtube.com/watch?v=abc123",
                                        "embedUrl", "https://www.youtube.com/embed/abc123"
                                ))
                        ),
                        Map.of(
                                "type", "mcq",
                                "question", "Why use a segment tree?",
                                "options", List.of("Fast range queries", "Only sorting", "Only strings"),
                                "answer", 0,
                                "explanation", "It supports efficient range queries."
                        )
                ),
                UUID.randomUUID(),
                "Range Queries",
                UUID.randomUUID(),
                "Segment Trees",
                null,
                null,
                null
        );
    }
}
