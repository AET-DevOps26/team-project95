package com.project95.thesis.thesis.service;

import com.project95.thesis.management.dto.*;
import com.project95.thesis.thesis.config.ClientProperties;
import com.project95.thesis.thesis.domain.ThesisProposal;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.openapitools.jackson.nullable.JsonNullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ThesisCoordinationService {

  private static final Logger log = LoggerFactory.getLogger(ThesisCoordinationService.class);

  private final ThesisManagementService thesisManagementService;
  private final ScrapeRunService scrapeRunService;
  private final RestClient restClient;
  private final ClientProperties clientProperties;

  public ThesisCoordinationService(
      ThesisManagementService thesisManagementService,
      ScrapeRunService scrapeRunService,
      RestClient restClient,
      ClientProperties clientProperties) {
    this.thesisManagementService = thesisManagementService;
    this.scrapeRunService = scrapeRunService;
    this.restClient = restClient;
    this.clientProperties = clientProperties;
  }

  public ChairThesesReplacementResponseDto executeScrapeIngestionPipeline(
      Long chairId, ChairThesesReplacementRequestDto request) {
    Objects.requireNonNull(chairId, "chairId must not be null");
    Objects.requireNonNull(request, "request must not be null");

    log.info("Starting multi-service pipeline execution for chairId {}", chairId);

    // 1. Replace relational data and log scrape run atomically
    IngestionResult ingestionResult =
        thesisManagementService.replaceThesesInDatabase(chairId, request);
    
    Long trackingScrapeRunId = ingestionResult.scrapeRunId();
    List<ThesisProposal> persistentTheses = ingestionResult.persistentTheses();

    // 2. Prepare Vector Search replacement request
    ReplaceChairVectorsRequestDto vectorRequest = new ReplaceChairVectorsRequestDto();
    vectorRequest.setScrapeRunId(trackingScrapeRunId);

    List<VectorThesisDocumentDto> vectorDocs =
        persistentTheses.stream()
            .map(
                entity -> {
                  VectorThesisDocumentDto doc = new VectorThesisDocumentDto();
                  doc.setThesisId(entity.getId());
                  doc.setChairId(chairId);
                  doc.setTitle(entity.getTitle());

                  doc.setDegreeType(JsonNullable.of(entity.getDegreeType()));
                  doc.setAiOverview(JsonNullable.of(entity.getAiOverview()));
                  doc.setOriginalDescription(JsonNullable.of(entity.getOriginalDescription()));

                  if (entity.getSourceUrl() != null) {
                    try {
                      doc.setSourceUrl(URI.create(entity.getSourceUrl()));
                    } catch (Exception e) {
                      log.warn(
                          "Failed to parse Source URL string to URI token format: {}",
                          entity.getSourceUrl());
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
                    doc.setResearchArea(JsonNullable.of(areaName));
                  }
                  return doc;
                })
            .collect(Collectors.toList());

    vectorRequest.setTheses(vectorDocs);

    int vectorReplacementsCount = 0;
    String vectorSyncError = null;
    try {
      String vectorSyncUrl =
          clientProperties.getVectorSearch().getUrl()
              + "/internal/v1/vector-search-service/chairs/"
              + chairId
              + "/index";

      ReplaceChairVectorsResponseDto vectorResponse =
          restClient
              .post()
              .uri(vectorSyncUrl)
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
              + " Updating scrape run to PARTIAL_SUCCESS. Reason: {}",
          chairId,
          e.getMessage());

      vectorSyncError = "Vector sync failed: " + e.getMessage();
      scrapeRunService.updateScrapeRunStatus(trackingScrapeRunId, "PARTIAL_SUCCESS", vectorSyncError);
    }

    ChairThesesReplacementResponseDto response = new ChairThesesReplacementResponseDto();
    response.setScrapeRunId(trackingScrapeRunId);
    response.setChairId(chairId);
    response.setInsertedRelationalTheses(persistentTheses.size());
    response.setReplacedVectorEntries(vectorReplacementsCount);
    response.setDeletedRelationalTheses((int) ingestionResult.deletedCount());

    if (vectorSyncError != null) {
      response.setErrorMessage(JsonNullable.of(vectorSyncError));
    }

    return response;
  }
}
