package com.texttolearn.pdf.dto;

public record LessonPdfResponse(
        byte[] pdf,
        String fileName,
        String contentType
) {
}
