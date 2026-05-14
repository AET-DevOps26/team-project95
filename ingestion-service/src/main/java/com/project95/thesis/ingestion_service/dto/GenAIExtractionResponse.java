package com.project95.thesis.ingestion_service.dto;

import java.util.List;

public record GenAIExtractionResponse(
    List<ThesisProposalInput> theses,
    String extractionNotes
) {}
