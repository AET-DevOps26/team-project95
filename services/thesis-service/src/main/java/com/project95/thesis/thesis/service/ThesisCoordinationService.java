package com.project95.thesis.thesis.service;

import com.project95.thesis.management.dto.ChairThesesReplacementRequest;
import com.project95.thesis.management.dto.ChairThesesReplacementResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Objects;

@Service
public class ThesisCoordinationService {

  private static final Logger log = LoggerFactory.getLogger(ThesisCoordinationService.class);

  private final ThesisManagementService thesisManagementService;
  private final RestClient restClient;
  private final String vectorSearchServiceUrl;

  public ThesisCoordinationService(
      ThesisManagementService thesisManagementService,
      RestClient restClient,
    @Value("${app.services.vector-search}") String vectorSearchServiceUrl) {
    this.thesisManagementService = thesisManagementService;
    this.restClient = restClient;
    this.vectorSearchServiceUrl = vectorSearchServiceUrl;
  }

  public ChairThesesReplacementResponse executeScrapeIngestionPipeline(Long chairId, ChairThesesReplacementRequest request) {
    Objects.requireNonNull(chairId, "chairId must not be null");
    Objects.requireNonNull(request, "request must not be null");

    log.info("Pipeline execution requested for chairId {} via vector search service {}", chairId, vectorSearchServiceUrl);
    throw new UnsupportedOperationException("Scrape ingestion pipeline not implemented yet");
  }

}
