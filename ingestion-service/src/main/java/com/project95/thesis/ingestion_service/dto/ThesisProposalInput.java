package com.project95.thesis.ingestion_service.dto;

import java.util.List;

public record ThesisProposalInput(
    String title,
    String degreeType,
    String originalDescription,
    String aiOverview,
    String researchArea,
    String sourceUrl,
    String rawHtmlSnapshot,
    Double extractionConfidence,
    String status, 
    List<AdvisorInput> advisors,
    List<String> tags
) {}
