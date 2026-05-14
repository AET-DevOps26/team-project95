package com.project95.thesis.ingestion_service.dto;

public record GenAIExtractionRequest(
    Long sourceEndpointId,
    Long chairId,
    String chairName,
    String sourceUrl,
    String rawHtml,
    String extractedPlainText
) {}
