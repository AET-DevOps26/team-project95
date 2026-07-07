package com.project95.thesis.thesis.service;

import com.project95.thesis.management.dto.*;
import com.project95.thesis.thesis.domain.ThesisProposal;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ThesisCoordinationService {

  private static final Logger log = LoggerFactory.getLogger(ThesisCoordinationService.class);

  private final ThesisManagementService thesisManagementService;
  private final RestClient restClient;
  private final MeterRegistry meterRegistry;

  public ThesisCoordinationService(
      ThesisManagementService thesisManagementService,
      RestClient restClient,
      MeterRegistry meterRegistry) {
    this.thesisManagementService = thesisManagementService;
    this.restClient = restClient;
    this.meterRegistry = meterRegistry;
  }

  public SourceEndpointThesesReplacementResponseDto executeScrapeIngestionPipeline(
      Long sourceEndpointId, SourceEndpointThesesReplacementRequestDto request) {

    Objects.requireNonNull(sourceEndpointId, "sourceEndpointId must not be null");
    Objects.requireNonNull(request, "request payload must not be null");

    int candidateCount = request.getTheses() == null ? 0 : request.getTheses().size();

    log.info(
        "Executing Scrape Ingestion Pipeline for sourceEndpointId: {}. Candidate count: {}",
        sourceEndpointId,
        candidateCount);

    long start = System.nanoTime();
    IngestionResult ingestionResult = null;
    List<ThesisProposal> persistentTheses = List.of();
    int vectorReplacementsCount = 0;
    String vectorSyncError = null;

    try {
      // 1. Transactional Database Update (Relational)
      ingestionResult = thesisManagementService.replaceThesesInDatabase(sourceEndpointId, request);
      persistentTheses = ingestionResult.persistentTheses();

      // 2. Prepare Vector Search replacement request
      ReplaceSourceEndpointVectorsRequestDto vectorRequest =
          new ReplaceSourceEndpointVectorsRequestDto();
      // ScrapeRunId is now optional and managed by the scraping-service

      List<VectorThesisDocumentDto> vectorDocs =
          persistentTheses.stream()
              .map(
                  entity -> {
                    VectorThesisDocumentDto doc = new VectorThesisDocumentDto();
                    doc.setThesisId(entity.getId());
                    doc.setChairId(entity.getChair().getId());
                    doc.setSourceEndpointId(sourceEndpointId);
                    doc.setTitle(entity.getTitle());

                    doc.setDegreeType(entity.getDegreeType());
                    doc.setAiOverview(entity.getAiOverview());
                    doc.setOriginalDescription(entity.getOriginalDescription());

                    if (entity.getSourceUrl() != null) {
                      try {
                        doc.setSourceUrl(URI.create(entity.getSourceUrl()));
                      } catch (Exception e) {
                        log.warn("Invalid source URL for thesis id: {}", entity.getId());
                      }
                    }

                    if (!entity.getResearchAreas().isEmpty()) {
                      String areaName = entity.getResearchAreas().iterator().next().getName();
                      doc.setResearchArea(areaName);
                    }
                    return doc;
                  })
              .collect(Collectors.toList());

      vectorRequest.setTheses(vectorDocs);

      try {
        ReplaceSourceEndpointVectorsResponseDto vectorResponse =
            restClient
                .post()
                .uri(
                    "/internal/v1/vector-search-service/source-endpoints/{sourceEndpointId}/index",
                    sourceEndpointId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(vectorRequest)
                .retrieve()
                .body(ReplaceSourceEndpointVectorsResponseDto.class);

        if (vectorResponse != null && vectorResponse.getInsertedVectorEntries() != null) {
          vectorReplacementsCount = vectorResponse.getInsertedVectorEntries();
        }
        log.info("Vector Search Service synchronizations finalized successfully.");

        if (request.getLastContentHash() != null) {
          thesisManagementService.updateLastContentHash(
              sourceEndpointId, request.getLastContentHash());
        }

      } catch (Exception e) {
        log.error(
            "CRITICAL: Relational data updated, but vector indexing sync failed for"
                + " sourceEndpointId: {}. Reason: {}",
            sourceEndpointId,
            e.getMessage());

        vectorSyncError = "Vector sync failed: " + e.getMessage();
      }

      SourceEndpointThesesReplacementResponseDto response =
          new SourceEndpointThesesReplacementResponseDto();
      response.setSourceEndpointId(sourceEndpointId);
      response.setInsertedRelationalTheses(persistentTheses.size());
      response.setReplacedVectorEntries(vectorReplacementsCount);
      response.setDeletedRelationalTheses((int) ingestionResult.deletedCount());

      if (vectorSyncError != null) {
        response.setErrorMessage(vectorSyncError);
      }

      return response;
    } finally {
      long duration = System.nanoTime() - start;
      meterRegistry
          .timer("thesis_ingestion_pipeline_duration_seconds")
          .record(duration, java.util.concurrent.TimeUnit.NANOSECONDS);

      String chairIdStr = "unknown";
      if (!persistentTheses.isEmpty()) {
        chairIdStr = String.valueOf(persistentTheses.get(0).getChair().getId());
      }

      if (!persistentTheses.isEmpty()) {
        meterRegistry
            .counter("thesis_ingested_total", "chair_id", chairIdStr, "action", "ingested")
            .increment(persistentTheses.size());
      }
      if (ingestionResult != null && ingestionResult.deletedCount() > 0) {
        meterRegistry
            .counter("thesis_ingested_total", "chair_id", chairIdStr, "action", "closed")
            .increment(ingestionResult.deletedCount());
      }
    }
  }
}
