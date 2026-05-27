package com.project95.thesis.thesis.service;

import com.project95.thesis.thesis.domain.ThesisProposal;
import java.util.List;

/**
 * Result of an atomic thesis ingestion and scrape run logging operation.
 */
public record IngestionResult(Long scrapeRunId, List<ThesisProposal> persistentTheses, long deletedCount) {}
