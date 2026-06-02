package com.project95.thesis.thesis.service;

import com.project95.thesis.thesis.domain.ThesisProposal;
import java.util.List;

/** Result of an atomic thesis ingestion operation. */
public record IngestionResult(List<ThesisProposal> persistentTheses, long deletedCount) {}
