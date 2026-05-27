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

  public ChairThesesReplacementResponse executeScrapeIngestionPipeline(
      Long chairId, ChairThesesReplacementRequest request) {
    Objects.requireNonNull(chairId, "chairId must not be null");
    Objects.requireNonNull(request, "request must not be null");

    log.info("Starting multi-service pipeline execution for chairId {}", chairId);

    // 1. Log the scrape run result as a persisted entity
    ScrapeRunLogRequest logRequest = new ScrapeRunLogRequest();
    logRequest.setSourceEndpointId(request.getSourceEndpointId());
    logRequest.setStartedAt(request.getStartedAt());
    logRequest.setFinishedAt(request.getFinishedAt());
    logRequest.setStatus(ScrapeRunLogRequest.StatusEnum.valueOf(request.getStatus().getValue()));
    logRequest.setErrorMessage(request.getErrorMessage());
    
    int candidates = request.getTheses() != null ? request.getTheses().size() : 0;
    logRequest.setCandidatesFound(JsonNullable.of(candidates));
    
    ScrapeRunLogResponse logResponse = scrapeRunService.logScrapeRun(logRequest);
    Long trackingScrapeRunId = logResponse.getId();

    // 2. Replace relational data
    List<ThesisProposal> persistentTheses =
        thesisManagementService.replaceThesesInDatabase(chairId, request);

    // 3. Prepare Vector Search replacement request
    ReplaceChairVectorsRequest vectorRequest = new ReplaceChairVectorsRequest();
    vectorRequest.setScrapeRunId(trackingScrapeRunId);

    List<VectorThesisDocument> vectorDocs =
        persistentTheses.stream()
            .map(
                entity -> {
                  VectorThesisDocument doc = new VectorThesisDocument();
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
    try {
      ReplaceChairVectorsResponse vectorResponse =
          restClient
              .post()
              .uri(
                  clientProperties.getVectorSearch().getUrl()
                      + "/internal/v1/vector-search-service/chairs/{chairId}/index",
                  chairId)
              .contentType(MediaType.APPLICATION_JSON)
              .body(vectorRequest)
              .retrieve()
              .body(ReplaceChairVectorsResponse.class);

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
      throw new RuntimeException("Vector Search index synchronization failed.", e);
    }

    ChairThesesReplacementResponse response = new ChairThesesReplacementResponse();
    response.setScrapeRunId(trackingScrapeRunId);
    response.setChairId(chairId);
    response.setInsertedRelationalTheses(persistentTheses.size());
    response.setReplacedVectorEntries(vectorReplacementsCount);
    response.setDeletedRelationalTheses(persistentTheses.size());

    return response;
  }
}
