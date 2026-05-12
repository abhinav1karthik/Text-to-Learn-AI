package com.texttolearn.pdf.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.texttolearn.course.dto.LessonResponse;
import com.texttolearn.pdf.dto.LessonPdfResponse;
import com.texttolearn.pdf.error.LessonPdfGenerationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class LessonPdfService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final Pattern BOLD_MARKDOWN_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");

    public LessonPdfResponse generateLessonPdf(LessonResponse lesson) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(renderLessonHtml(lesson), null);
            builder.toStream(outputStream);
            builder.run();

            return new LessonPdfResponse(
                    outputStream.toByteArray(),
                    safeFileName(lesson.title()) + ".pdf",
                    PDF_CONTENT_TYPE
            );
        } catch (IOException exception) {
            throw new LessonPdfGenerationException("Failed to close lesson PDF stream.", exception);
        } catch (RuntimeException exception) {
            throw new LessonPdfGenerationException("Failed to generate lesson PDF.", exception);
        }
    }

    String renderLessonHtml(LessonResponse lesson) {
        StringBuilder html = new StringBuilder();
        html.append("""
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8" />
                  <style>
                    @page {
                      size: A4;
                      margin: 28mm 20mm;
                    }
                    * {
                      box-sizing: border-box;
                    }
                    body {
                      margin: 0;
                      color: #172026;
                      background: #ffffff;
                      font-family: Inter, Arial, sans-serif;
                      font-size: 12.5px;
                      line-height: 1.58;
                    }
                    .document-header {
                      border-bottom: 2px solid #1d4ed8;
                      padding-bottom: 16px;
                      margin-bottom: 22px;
                    }
                    .eyebrow {
                      margin: 0 0 6px;
                      color: #1d4ed8;
                      font-size: 10px;
                      font-weight: 700;
                      letter-spacing: 1.8px;
                      text-transform: uppercase;
                    }
                    h1 {
                      margin: 0 0 8px;
                      color: #111827;
                      font-size: 28px;
                      line-height: 1.15;
                    }
                    .meta {
                      margin: 0;
                      color: #52616d;
                      font-size: 12px;
                    }
                    .section {
                      page-break-inside: avoid;
                      margin: 0 0 16px;
                      border: 1px solid #dbe3ea;
                      border-radius: 8px;
                      padding: 16px;
                    }
                    .section h2 {
                      margin: 0 0 10px;
                      color: #111827;
                      font-size: 18px;
                    }
                    .section h3 {
                      margin: 0 0 8px;
                      color: #111827;
                      font-size: 15px;
                    }
                    p {
                      margin: 0;
                      color: #334155;
                    }
                    ul {
                      margin: 0;
                      padding-left: 18px;
                    }
                    li {
                      margin-bottom: 6px;
                    }
                    pre {
                      margin: 0;
                      border-radius: 8px;
                      background: #0f172a;
                      color: #e5e7eb;
                      padding: 14px;
                      white-space: pre-wrap;
                      word-wrap: break-word;
                      font-family: "Courier New", monospace;
                      font-size: 10.5px;
                      line-height: 1.45;
                    }
                    .code-language,
                    .video-label,
                    .answer-label {
                      display: block;
                      margin-bottom: 8px;
                      color: #64748b;
                      font-size: 10px;
                      font-weight: 700;
                      letter-spacing: 1.4px;
                      text-transform: uppercase;
                    }
                    .video-query {
                      border-left: 3px solid #1d4ed8;
                      background: #eff6ff;
                      padding: 10px 12px;
                    }
                    .video-link {
                      display: block;
                      margin-top: 8px;
                      color: #1d4ed8;
                      word-wrap: break-word;
                      text-decoration: underline;
                    }
                    .mcq-options {
                      margin-top: 10px;
                    }
                    .mcq-option {
                      margin-bottom: 6px;
                      border: 1px solid #dbe3ea;
                      border-radius: 6px;
                      padding: 8px 10px;
                    }
                    .mcq-option.correct {
                      border-color: #86efac;
                      background: #f0fdf4;
                    }
                    .answer {
                      margin-top: 10px;
                      color: #166534;
                      font-weight: 700;
                    }
                    .explanation {
                      margin-top: 8px;
                      color: #475569;
                    }
                  </style>
                </head>
                <body>
                """);

        html.append("<header class=\"document-header\">");
        html.append("<p class=\"eyebrow\">Text To Learn Lesson</p>");
        html.append("<h1>").append(escapeHtml(lesson.title())).append("</h1>");
        html.append("<p class=\"meta\">")
                .append(escapeHtml(lesson.courseTitle()))
                .append(" / ")
                .append(escapeHtml(lesson.moduleTitle()))
                .append("</p>");
        html.append("</header>");

        renderObjectives(html, lesson.objectives());
        renderContent(html, lesson.content());

        html.append("</body></html>");
        return html.toString();
    }

    private void renderObjectives(StringBuilder html, List<String> objectives) {
        if (objectives == null || objectives.isEmpty()) {
            return;
        }

        html.append("<section class=\"section\">");
        html.append("<h2>Objectives</h2>");
        html.append("<ul>");
        objectives.forEach(objective -> html.append("<li>").append(richText(objective)).append("</li>"));
        html.append("</ul>");
        html.append("</section>");
    }

    private void renderContent(StringBuilder html, List<Map<String, Object>> content) {
        if (content == null || content.isEmpty()) {
            html.append("<section class=\"section\"><p>This lesson does not have generated content yet.</p></section>");
            return;
        }

        for (Map<String, Object> block : content) {
            renderBlock(html, block);
        }
    }

    private void renderBlock(StringBuilder html, Map<String, Object> block) {
        if (block == null) {
            return;
        }

        String type = stringValue(block.get("type")).toLowerCase(Locale.ROOT);
        switch (type) {
            case "heading" -> renderHeadingBlock(html, block);
            case "paragraph" -> renderParagraphBlock(html, block);
            case "code" -> renderCodeBlock(html, block);
            case "video" -> renderVideoBlock(html, block);
            case "mcq" -> renderMcqBlock(html, block);
            default -> renderParagraphBlock(html, block);
        }
    }

    private void renderHeadingBlock(StringBuilder html, Map<String, Object> block) {
        html.append("<section class=\"section\">");
        html.append("<h2>").append(richText(stringValue(block.get("text")))).append("</h2>");
        html.append("</section>");
    }

    private void renderParagraphBlock(StringBuilder html, Map<String, Object> block) {
        html.append("<section class=\"section\">");
        html.append("<p>").append(richText(stringValue(block.get("text")))).append("</p>");
        html.append("</section>");
    }

    private void renderCodeBlock(StringBuilder html, Map<String, Object> block) {
        html.append("<section class=\"section\">");
        html.append("<span class=\"code-language\">")
                .append(escapeHtml(stringValue(block.getOrDefault("language", "code"))))
                .append("</span>");
        html.append("<pre>").append(escapeHtml(stringValue(block.get("text")))).append("</pre>");
        html.append("</section>");
    }

    private void renderVideoBlock(StringBuilder html, Map<String, Object> block) {
        String query = stringValue(block.get("query"));
        if (query.isBlank()) {
            query = stringValue(block.get("url"));
        }
        List<String> youtubeUrls = youtubeUrls(block, query);

        html.append("<section class=\"section\">");
        html.append("<span class=\"video-label\">Related video</span>");
        if (!query.isBlank()) {
            html.append("<p class=\"video-query\">").append(richText(query)).append("</p>");
        }
        for (String youtubeUrl : youtubeUrls) {
            html.append("<a class=\"video-link\" href=\"")
                    .append(escapeHtml(youtubeUrl))
                    .append("\">")
                    .append(escapeHtml(youtubeUrl))
                    .append("</a>");
        }
        html.append("</section>");
    }

    private List<String> youtubeUrls(Map<String, Object> block, String query) {
        if (block.get("videos") instanceof List<?> videos && !videos.isEmpty()) {
            return videos.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(this::youtubeUrlFromVideo)
                    .filter(url -> !url.isBlank())
                    .toList();
        }

        String fallbackUrl = youtubeUrlFromVideo(block);
        if (!fallbackUrl.isBlank()) {
            return List.of(fallbackUrl);
        }

        if (!query.isBlank()) {
            return List.of("https://www.youtube.com/results?search_query="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8));
        }

        return List.of();
    }

    private String youtubeUrlFromVideo(Map<?, ?> block) {
        String directUrl = firstPresent(
                block.get("watchUrl"),
                block.get("url"),
                block.get("embedUrl")
        );
        if (!directUrl.isBlank()) {
            return toWatchUrl(directUrl);
        }

        String videoId = stringValue(block.get("videoId"));
        if (!videoId.isBlank()) {
            return "https://www.youtube.com/watch?v=" + videoId;
        }

        return "";
    }

    private String toWatchUrl(String url) {
        if (url.contains("youtube.com/embed/")) {
            String videoId = url.substring(url.lastIndexOf('/') + 1);
            return "https://www.youtube.com/watch?v=" + videoId;
        }

        return url;
    }

    private String firstPresent(Object... values) {
        for (Object value : values) {
            String text = stringValue(value);
            if (!text.isBlank()) {
                return text;
            }
        }

        return "";
    }

    private void renderMcqBlock(StringBuilder html, Map<String, Object> block) {
        html.append("<section class=\"section\">");
        html.append("<h3>").append(richText(stringValue(block.get("question")))).append("</h3>");
        html.append("<div class=\"mcq-options\">");

        List<?> options = block.get("options") instanceof List<?> optionList ? optionList : List.of();
        int answerIndex = answerIndex(block.get("answer"));
        for (int index = 0; index < options.size(); index++) {
            String className = index == answerIndex ? "mcq-option correct" : "mcq-option";
            html.append("<div class=\"").append(className).append("\">")
                    .append((char) ('A' + index))
                    .append(". ")
                    .append(richText(stringValue(options.get(index))))
                    .append("</div>");
        }

        html.append("</div>");
        if (answerIndex >= 0 && answerIndex < options.size()) {
            html.append("<p class=\"answer\"><span class=\"answer-label\">Correct answer</span>")
                    .append((char) ('A' + answerIndex))
                    .append(". ")
                    .append(richText(stringValue(options.get(answerIndex))))
                    .append("</p>");
        }

        String explanation = stringValue(block.get("explanation"));
        if (!explanation.isBlank()) {
            html.append("<p class=\"explanation\">").append(richText(explanation)).append("</p>");
        }
        html.append("</section>");
    }

    private int answerIndex(Object answer) {
        if (answer instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(stringValue(answer));
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private String richText(String value) {
        String escaped = escapeHtml(value);
        Matcher matcher = BOLD_MARKDOWN_PATTERN.matcher(escaped);
        String withBold = matcher.replaceAll("<strong>$1</strong>");
        return withBold.replace("\n", "<br />");
    }

    private String escapeHtml(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String safeFileName(String value) {
        String normalized = value == null ? "lesson" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (normalized.isBlank()) {
            return "lesson";
        }

        return normalized;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return "";
        }

        return String.valueOf(value).trim();
    }
}
