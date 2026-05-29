package com.project95.thesis.thesis.service;

import com.project95.thesis.management.dto.*;
import com.project95.thesis.thesis.config.ClientProperties;
import com.project95.thesis.thesis.domain.ThesisProposal;
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

  public ThesisCoordinationService(
      ThesisManagementService thesisManagementService, RestClient restClient) {
    this.thesisManagementService = thesisManagementService;
    this.restClient = restClient;
  }

  public ChairThesesReplacementResponseDto executeScrapeIngestionPipeline(
      Long chairId, ChairThesesReplacementRequestDto request) {

    Objects.requireNonNull(chairId, "chairId must not be null");
    Objects.requireNonNull(request, "request payload must not be null");

    log.info(
        "Executing Scrape Ingestion Pipeline for chairId: {}. Candidate count: {}",
        chairId,
        request.getTheses().size());

    // 1. Transactional Database Update (Relational)
    IngestionResult ingestionResult =
        thesisManagementService.replaceThesesInDatabase(chairId, request);
    List<ThesisProposal> persistentTheses = ingestionResult.persistentTheses();

    // 2. Prepare Vector Search replacement request
    ReplaceChairVectorsRequestDto vectorRequest = new ReplaceChairVectorsRequestDto();
    // ScrapeRunId is now optional and managed by the scraping-service
    vectorRequest.setScrapeRunId(null);

    List<VectorThesisDocumentDto> vectorDocs =
        persistentTheses.stream()
            .map(
                entity -> {
                  VectorThesisDocumentDto doc = new VectorThesisDocumentDto();
                  doc.setThesisId(entity.getId());
                  doc.setChairId(chairId);
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

                  if (!entity.getTags().isEmpty()) {
                    doc.setTags(
                        entity.getTags().stream()
                            .map(t -> t.getName())
                            .collect(Collectors.toList()));
                  }

                  if (!entity.getResearchAreas().isEmpty()) {
                    String areaName = entity.getResearchAreas().iterator().next().getName();
                    doc.setResearchArea(areaName);
                  }
                  return doc;
                })
            .collect(Collectors.toList());

    vectorRequest.setTheses(vectorDocs);

    int vectorReplacementsCount = 0;
    String vectorSyncError = null;
    try {
      ReplaceChairVectorsResponseDto vectorResponse =
          restClient
              .post()
              .uri("/internal/v1/vector-search-service/chairs/{chairId}/index", chairId)
              .contentType(MediaType.APPLICATION_JSON)
              .body(vectorRequest)
              .retrieve()
              .body(ReplaceChairVectorsResponseDto.class);

      if (vectorResponse != null && vectorResponse.getInsertedVectorEntries() != null) {
        vectorReplacementsCount = vectorResponse.getInsertedVectorEntries();
      }
      log.info("Vector Search Service synchronizations finalized successfully.");

    } catch (Exception e) {
      log.error(
          "CRITICAL: Relational data updated, but vector indexing sync failed for chairId: {}."
              + " Reason: {}",
          chairId,
          e.getMessage());

      vectorSyncError = "Vector sync failed: " + e.getMessage();
    }

    ChairThesesReplacementResponseDto response = new ChairThesesReplacementResponseDto();
    response.setScrapeRunId(null);
    response.setChairId(chairId);
    response.setInsertedRelationalTheses(persistentTheses.size());
    response.setReplacedVectorEntries(vectorReplacementsCount);
    response.setDeletedRelationalTheses((int) ingestionResult.deletedCount());

    if (vectorSyncError != null) {
      response.setErrorMessage(vectorSyncError);
    }

    return response;
  }
}
