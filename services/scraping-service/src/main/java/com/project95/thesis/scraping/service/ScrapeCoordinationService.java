package com.project95.thesis.scraping.service;

import com.project95.thesis.scraping.config.ClientProperties;
import com.project95.thesis.scraping.dto.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.openapitools.jackson.nullable.JsonNullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ScrapeCoordinationService {

  private static final Logger log = LoggerFactory.getLogger(ScrapeCoordinationService.class);

  private final RestClient restClient;
  private final ClientProperties clientProperties;

  public ScrapeCoordinationService(RestClient restClient, ClientProperties clientProperties) {
    this.restClient = restClient;
    this.clientProperties = clientProperties;
  }

  @Scheduled(cron = "${app.scheduling.scrape-cron}")
  public void runScrapeCycle() {
    log.info("Starting scrape cycle...");

    String endpointsUrl =
        clientProperties.getMainThesis().getUrl() + "/internal/v1/thesis-service/source-endpoints";
    SourceEndpointListResponse response = null;
    try {
      response =
          restClient.get().uri(endpointsUrl).retrieve().body(SourceEndpointListResponse.class);
    } catch (Exception e) {
      log.error("Failed to fetch source endpoints from Main Thesis Service", e);
      return;
    }

    if (response == null || response.getEndpoints() == null || response.getEndpoints().isEmpty()) {
      log.info("No source endpoints found to scrape.");
      return;
    }

    for (SourceEndpoint endpoint : response.getEndpoints()) {
      processEndpoint(endpoint);
    }

    log.info("Scrape cycle completed.");
  }

  private void processEndpoint(SourceEndpoint endpoint) {
    log.info("Scraping endpoint: {} (Chair: {})", endpoint.getUrl(), endpoint.getChairName());

    OffsetDateTime startedAt = OffsetDateTime.now(ZoneOffset.UTC);
    GenAIExtractionResponse genAiResponse = null;

    try {
      String rawHtml = restClient.get().uri(endpoint.getUrl()).retrieve().body(String.class);

      if (rawHtml == null || rawHtml.isBlank()) {
        throw new RuntimeException("Received empty HTML from source URL");
      }

      GenAIExtractionRequest genAiRequest = new GenAIExtractionRequest();
      genAiRequest.setSourceEndpointId(endpoint.getId());
      genAiRequest.setChairId(endpoint.getChairId());
      genAiRequest.setChairName(JsonNullable.of(endpoint.getChairName()));
      genAiRequest.setSourceUrl(endpoint.getUrl());
      genAiRequest.setRawHtml(rawHtml);

      String extractUrl =
          clientProperties.getGenAi().getUrl() + "/internal/v1/genai-service/extract-theses";

      genAiResponse =
          restClient
              .post()
              .uri(extractUrl)
              .body(genAiRequest)
              .retrieve()
              .body(GenAIExtractionResponse.class);

      if (genAiResponse == null || genAiResponse.getTheses() == null) {
        throw new RuntimeException("GenAI extraction returned null response");
      }

      OffsetDateTime finishedAt = OffsetDateTime.now(ZoneOffset.UTC);

      // Replace Theses (Main Thesis Service will now log the successful scrape run)
      ChairThesesReplacementRequest requestBody = new ChairThesesReplacementRequest();
      requestBody.setSourceEndpointId(endpoint.getId());
      requestBody.setStartedAt(startedAt);
      requestBody.setFinishedAt(finishedAt);
      requestBody.setStatus(ChairThesesReplacementRequest.StatusEnum.SUCCESS);
      requestBody.setTheses(genAiResponse.getTheses());

      submitThesesReplacement(endpoint.getChairId(), requestBody);

    } catch (Exception e) {
      log.error(
          "Failed to process endpoint ID {}. Logging failure and skipping thesis replacement.",
          endpoint.getId(),
          e);
      OffsetDateTime finishedAt = OffsetDateTime.now(ZoneOffset.UTC);
      logScrapeRun(
          endpoint.getId(),
          startedAt,
          finishedAt,
          ScrapeRunLogRequest.StatusEnum.FAILED,
          e.getMessage(),
          0);
    }
  }

  private void logScrapeRun(
      Long endpointId,
      OffsetDateTime startedAt,
      OffsetDateTime finishedAt,
      ScrapeRunLogRequest.StatusEnum status,
      String error,
      Integer candidates) {
    ScrapeRunLogRequest logRequest = new ScrapeRunLogRequest();
    logRequest.setSourceEndpointId(endpointId);
    logRequest.setStartedAt(startedAt);
    logRequest.setFinishedAt(finishedAt);
    logRequest.setStatus(status);
    logRequest.setErrorMessage(JsonNullable.of(error));
    logRequest.setCandidatesFound(JsonNullable.of(candidates));

    String logUrl =
        clientProperties.getMainThesis().getUrl() + "/internal/v1/thesis-service/scrape-runs";
    try {
      restClient.post().uri(logUrl).body(logRequest).retrieve().toBodilessEntity();
      log.info("Successfully logged scrape run status {} for endpointId: {}", status, endpointId);
    } catch (Exception e) {
      log.error("Failed to log scrape run for endpointId: {}", endpointId, e);
    }
  }

  private void submitThesesReplacement(Long chairId, ChairThesesReplacementRequest submission) {
    String submitUrl =
        clientProperties.getMainThesis().getUrl()
            + "/internal/v1/thesis-service/chairs/"
            + chairId
            + "/theses";
    try {
      restClient.put().uri(submitUrl).body(submission).retrieve().toBodilessEntity();
      log.info("Successfully submitted theses replacement for chairId: {}", chairId);
    } catch (Exception e) {
      log.error("Failed to submit theses replacement for chairId: {}", chairId, e);
    }
  }
}
