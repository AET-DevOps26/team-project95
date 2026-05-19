package com.project95.thesis.ingestion_service.dto;

import java.time.OffsetDateTime;

public record SourceEndpoint(
        Long id,
        Long chairId,
        String chairName,
        String url,
        String status,
        OffsetDateTime lastScrapedAt
) {}
