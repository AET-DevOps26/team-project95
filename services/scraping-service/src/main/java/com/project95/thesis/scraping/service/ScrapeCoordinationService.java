package com.project95.thesis.scraping.service;

import com.project95.thesis.scraping.config.ClientProperties;
import com.project95.thesis.scraping.dto.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ScrapeCoordinationService {

  private static final Logger log = LoggerFactory.getLogger(ScrapeCoordinationService.class);

  private final RestClient thesisServiceClient;
  private final RestClient genAiServiceClient;
  private final RestClient scrapingClient;

  public ScrapeCoordinationService(
      @Qualifier("thesisServiceClient") RestClient thesisServiceClient,
      @Qualifier("genAiServiceClient") RestClient genAiServiceClient,
      @Qualifier("scrapingClient") RestClient scrapingClient,
      ClientProperties clientProperties) {
    this.thesisServiceClient = thesisServiceClient;
    this.genAiServiceClient = genAiServiceClient;
    this.scrapingClient = scrapingClient;
  }

  @Scheduled(cron = "${app.scheduling.scrape-cron}")
  public void runScrapeCycle() {
    log.info("Starting scrape cycle...");

    SourceEndpointListResponseDto response = null;
    try {
      response =
          thesisServiceClient
              .get()
              .uri("/internal/v1/thesis-service/source-endpoints")
              .retrieve()
              .body(SourceEndpointListResponseDto.class);
    } catch (Exception e) {
      log.error("Failed to fetch source endpoints from Main Thesis Service", e);
      return;
    }

    if (response == null || response.getEndpoints() == null || response.getEndpoints().isEmpty()) {
      log.info("No source endpoints found to scrape.");
      return;
    }

    for (SourceEndpointDto endpoint : response.getEndpoints()) {
      processEndpoint(endpoint);
    }

    log.info("Scrape cycle completed.");
  }

  private void processEndpoint(SourceEndpointDto endpoint) {
    log.info("Scraping endpoint: {} (Chair: {})", endpoint.getUrl(), endpoint.getChairName());

    OffsetDateTime startedAt = OffsetDateTime.now(ZoneOffset.UTC);
    GenAIExtractionResponseDto genAiResponse = null;

    try {
      String rawHtml = scrapingClient.get().uri(endpoint.getUrl()).retrieve().body(String.class);

      if (rawHtml == null || rawHtml.isBlank()) {
        throw new RuntimeException("Received empty HTML from source URL");
      }

      GenAIExtractionRequestDto genAiRequest = new GenAIExtractionRequestDto();
      genAiRequest.setSourceEndpointId(endpoint.getId());
      genAiRequest.setChairId(endpoint.getChairId());
      genAiRequest.setChairName(endpoint.getChairName());
      genAiRequest.setSourceUrl(endpoint.getUrl());
      genAiRequest.setRawHtml(rawHtml);

      genAiResponse =
          genAiServiceClient
              .post()
              .uri("/internal/v1/genai-service/extract-theses")
              .body(genAiRequest)
              .retrieve()
              .body(GenAIExtractionResponseDto.class);

      if (genAiResponse == null || genAiResponse.getTheses() == null) {
        throw new RuntimeException("GenAI extraction returned null response");
      }

      OffsetDateTime finishedAt = OffsetDateTime.now(ZoneOffset.UTC);

      // Replace Theses
      SourceEndpointThesesReplacementRequestDto requestBody = new SourceEndpointThesesReplacementRequestDto();
      requestBody.setTheses(genAiResponse.getTheses());

      submitThesesReplacement(endpoint.getId(), requestBody);

      // Log SUCCESS (Centralized responsibility)
      logScrapeRun(
          endpoint.getId(),
          startedAt,
          finishedAt,
          ScrapeRunLogRequestDto.StatusEnum.SUCCESS,
          null,
          genAiResponse.getTheses().size());

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
          ScrapeRunLogRequestDto.StatusEnum.FAILED,
          e.getMessage(),
          0);
    }
  }

  private void logScrapeRun(
      Long endpointId,
      OffsetDateTime startedAt,
      OffsetDateTime finishedAt,
      ScrapeRunLogRequestDto.StatusEnum status,
      String error,
      Integer candidates) {
    ScrapeRunLogRequestDto logRequest = new ScrapeRunLogRequestDto();
    logRequest.setSourceEndpointId(endpointId);
    logRequest.setStartedAt(startedAt);
    logRequest.setFinishedAt(finishedAt);
    logRequest.setStatus(status);
    logRequest.setErrorMessage(error);
    logRequest.setCandidatesFound(candidates);

    try {
      thesisServiceClient
          .post()
          .uri("/internal/v1/thesis-service/scrape-runs")
          .body(logRequest)
          .retrieve()
          .toBodilessEntity();
      log.info("Successfully logged scrape run status {} for endpointId: {}", status, endpointId);
    } catch (Exception e) {
      log.error("Failed to log scrape run for endpointId: {}", endpointId, e);
    }
  }

  private void submitThesesReplacement(Long sourceEndpointId, SourceEndpointThesesReplacementRequestDto submission) {
    try {
      thesisServiceClient
          .put()
          .uri("/internal/v1/thesis-service/source-endpoints/" + sourceEndpointId + "/theses")
          .body(submission)
          .retrieve()
          .toBodilessEntity();
      log.info("Successfully submitted theses replacement for sourceEndpointId: {}", sourceEndpointId);
    } catch (Exception e) {
      log.error("Failed to submit theses replacement for sourceEndpointId: {}", sourceEndpointId, e);
      if (e instanceof RuntimeException re) throw re;
      throw new RuntimeException("Submission failed", e);
    }
  }
}
