package com.project95.thesis.ingestion_service.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ScrapeRunSubmission(
    Long sourceEndpointId,
    Long chairId,
    OffsetDateTime startedAt,
    OffsetDateTime finishedAt,
    ScrapeStatus status,
    String errorMessage,
    String rawHtmlSnapshotUrl,
    List<ThesisProposalInput> theses
) {}
