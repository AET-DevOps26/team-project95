package com.project95.thesis.ingestion_service.dto;

import java.util.List;

public record SourceEndpointListResponse(
    List<SourceEndpoint> endpoints
) {}
