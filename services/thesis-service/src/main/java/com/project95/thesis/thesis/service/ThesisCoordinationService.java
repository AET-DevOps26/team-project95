package com.project95.thesis.thesis.service;

import com.project95.thesis.management.dto.*;
import com.project95.thesis.thesis.domain.ThesisProposal;
import org.openapitools.jackson.nullable.JsonNullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ThesisCoordinationService {

  private static final Logger log = LoggerFactory.getLogger(ThesisCoordinationService.class);

  private final ThesisManagementService thesisManagementService;
  private final RestClient restClient;
  private final String vectorSearchServiceUrl;

  public ThesisCoordinationService(
      ThesisManagementService thesisManagementService,
      RestClient restClient,
      @Value("${app.services.vector-search:http://vector-search-service:8082}") String vectorSearchServiceUrl) {
    this.thesisManagementService = thesisManagementService;
    this.restClient = restClient;
    this.vectorSearchServiceUrl = vectorSearchServiceUrl;
  }

  public ChairThesesReplacementResponse executeScrapeIngestionPipeline(Long chairId, ChairThesesReplacementRequest request) {
    Objects.requireNonNull(chairId, "chairId must not be null");
    Objects.requireNonNull(request, "request must not be null");

    log.info("Starting multi-service pipeline execution for chairId {}", chairId);

    List<ThesisProposal> persistentTheses = thesisManagementService.replaceThesesInDatabase(chairId, request);

    ReplaceChairVectorsRequest vectorRequest = new ReplaceChairVectorsRequest();
    long trackingScrapeRunId = (long) (Math.random() * 10000);
    vectorRequest.setScrapeRunId(trackingScrapeRunId);

    List<VectorThesisDocument> vectorDocs = persistentTheses.stream().map(entity -> {
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
          log.warn("Failed to parse Source URL string to URI token format: {}", entity.getSourceUrl());
        }
      }
      
      if (!entity.getTags().isEmpty()) {
        doc.setTags(entity.getTags().stream().map(t -> t.getName()).collect(Collectors.toList()));
      }
      
      if (!entity.getResearchAreas().isEmpty()) {
        String areaName = entity.getResearchAreas().iterator().next().getName();
        doc.setResearchArea(JsonNullable.of(areaName));
      }
      return doc;
    }).collect(Collectors.toList());

    vectorRequest.setTheses(vectorDocs);

    int vectorReplacementsCount = 0;
    try {
      ReplaceChairVectorsResponse vectorResponse = restClient.post()
        .uri(vectorSearchServiceUrl + "/internal/v1/vector-search-service/chairs/{chairId}/index", chairId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(vectorRequest)
        .retrieve()
        .body(ReplaceChairVectorsResponse.class);

      if (vectorResponse != null && vectorResponse.getInsertedVectorEntries() != null) {
        vectorReplacementsCount = vectorResponse.getInsertedVectorEntries();
      }
      log.info("Vector Search Service synchronizations finalized successfully.");
      
    } catch (Exception e) {
      log.error("CRITICAL: Relational data updated, but vector indexing sync failed for chairId: {}. Reason: {}", chairId, e.getMessage());
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
