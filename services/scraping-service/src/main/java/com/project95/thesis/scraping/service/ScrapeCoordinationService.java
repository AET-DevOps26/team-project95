package com.project95.thesis.scraping.service;

import com.project95.thesis.scraping.config.ClientProperties;
import com.project95.thesis.scraping.dto.*;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ScrapeCoordinationService {

  private static final Logger log = LoggerFactory.getLogger(ScrapeCoordinationService.class);
  private static final Pattern BASIC_EMAIL_PATTERN =
      Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

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
    String rawHtml = null;

    try {
      rawHtml = scrapingClient.get().uri(endpoint.getUrl()).retrieve().body(String.class);

      if (rawHtml == null || rawHtml.isBlank()) {
        throw new RuntimeException("Received empty HTML from source URL");
      }

      // Query thesis-service to detect if there was a hash change
      DetectChangesRequestDto detectRequest = new DetectChangesRequestDto();
      detectRequest.setRawHtml(rawHtml);

      DetectChangesResponseDto detectResponse =
          thesisServiceClient
              .post()
              .uri("/thesis-internal/v1/source-endpoints/" + endpoint.getId() + "/detect-changes")
              .body(detectRequest)
              .retrieve()
              .body(DetectChangesResponseDto.class);

      if (detectResponse != null && Boolean.FALSE.equals(detectResponse.getChanged())) {
        log.info(
            "Endpoint content is unchanged (hash matches) for {}. Skipping GenAI extraction.",
            endpoint.getUrl());
        OffsetDateTime finishedAt = OffsetDateTime.now(ZoneOffset.UTC);

        logScrapeRun(
            endpoint.getId(),
            startedAt,
            finishedAt,
            ScrapeRunLogRequestDto.StatusEnum.SUCCESS,
            null,
            rawHtml,
            0);
        return;
      }

      // Content changed, proceed with GenAI extraction using token-optimized sanitized HTML
      String sanitizedHtml =
          (detectResponse != null && detectResponse.getSanitizedHtml() != null)
              ? detectResponse.getSanitizedHtml()
              : rawHtml;

      GenAIExtractionRequestDto genAiRequest = new GenAIExtractionRequestDto();
      genAiRequest.setSourceEndpointId(endpoint.getId());
      genAiRequest.setChairId(endpoint.getChairId());
      genAiRequest.setChairName(endpoint.getChairName());
      genAiRequest.setSourceUrl(endpoint.getUrl());
      genAiRequest.setRawHtml(sanitizedHtml);

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
      SourceEndpointThesesReplacementRequestDto requestBody =
          new SourceEndpointThesesReplacementRequestDto();
      sanitizeThesesForSubmission(genAiResponse.getTheses());
      requestBody.setTheses(genAiResponse.getTheses());
      if (detectResponse != null) {
        requestBody.setLastContentHash(detectResponse.getContentHash());
      }

      submitThesesReplacement(endpoint.getId(), requestBody);

      // Log SUCCESS (Centralized responsibility)
      logScrapeRun(
          endpoint.getId(),
          startedAt,
          finishedAt,
          ScrapeRunLogRequestDto.StatusEnum.SUCCESS,
          null,
          rawHtml,
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
          rawHtml,
          0);
    }
  }

  private void logScrapeRun(
      Long endpointId,
      OffsetDateTime startedAt,
      OffsetDateTime finishedAt,
      ScrapeRunLogRequestDto.StatusEnum status,
      String error,
      String rawHtml,
      Integer candidates) {
    ScrapeRunLogRequestDto logRequest = new ScrapeRunLogRequestDto();
    logRequest.setSourceEndpointId(endpointId);
    logRequest.setStartedAt(startedAt);
    logRequest.setFinishedAt(finishedAt);
    logRequest.setStatus(status);
    logRequest.setErrorMessage(error);
    logRequest.setRawHtmlSnapshot(rawHtml);
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

  private void sanitizeThesesForSubmission(List<ThesisProposalInputDto> theses) {
    if (theses == null) {
      return;
    }

    for (ThesisProposalInputDto thesis : theses) {
      thesis.setTitle(cleanText(thesis.getTitle()));
      thesis.setDegreeType(cleanText(thesis.getDegreeType()));
      thesis.setOriginalDescription(cleanText(thesis.getOriginalDescription()));
      thesis.setAiOverview(cleanText(thesis.getAiOverview()));
      thesis.setResearchArea(cleanText(thesis.getResearchArea()));
      thesis.setSourceUrl(cleanUri(thesis.getSourceUrl()));
      thesis.setStatus(cleanText(thesis.getStatus()));

      if (thesis.getTags() != null) {
        thesis.setTags(thesis.getTags().stream().map(this::cleanText).toList());
      }

      sanitizeAdvisorsWithoutUsableEmail(thesis);
    }
  }

  private void sanitizeAdvisorsWithoutUsableEmail(ThesisProposalInputDto thesis) {
    if (thesis.getAdvisors() == null || thesis.getAdvisors().isEmpty()) {
      return;
    }

    int originalAdvisorCount = thesis.getAdvisors().size();
    List<AdvisorInputDto> advisorsWithUsableEmail =
        thesis.getAdvisors().stream()
            .filter(advisor -> advisor != null && hasUsableEmail(advisor.getEmail()))
            .peek(
                advisor -> {
                  advisor.setEmail(cleanText(advisor.getEmail()));
                  advisor.setName(cleanAdvisorName(advisor.getName(), advisor.getEmail()));
                  advisor.setProfileUrl(cleanUri(advisor.getProfileUrl()));
                })
            .toList();

    if (advisorsWithUsableEmail.size() != originalAdvisorCount) {
      log.warn(
          "Dropping {} advisor(s) without usable email for thesis '{}'. Thesis will still be"
              + " submitted.",
          originalAdvisorCount - advisorsWithUsableEmail.size(),
          thesis.getTitle());
    }

    thesis.setAdvisors(advisorsWithUsableEmail);
  }

  private boolean hasUsableEmail(String email) {
    return email != null && BASIC_EMAIL_PATTERN.matcher(email.trim()).matches();
  }

  private String cleanAdvisorName(String name, String fallbackEmail) {
    String cleanedName = cleanText(name);
    return cleanedName == null || cleanedName.isBlank() ? fallbackEmail : cleanedName;
  }

  private URI cleanUri(URI value) {
    if (value == null) {
      return null;
    }

    String cleanedValue = cleanText(value.toString());
    return URI.create(cleanedValue);
  }

  private String cleanText(String value) {
    return value == null ? null : value.replace("\u0000", "");
  }

  private void submitThesesReplacement(
      Long sourceEndpointId, SourceEndpointThesesReplacementRequestDto submission) {
    try {
      thesisServiceClient
          .put()
          .uri("/internal/v1/thesis-service/source-endpoints/" + sourceEndpointId + "/theses")
          .body(submission)
          .retrieve()
          .toBodilessEntity();
      log.info(
          "Successfully submitted theses replacement for sourceEndpointId: {}", sourceEndpointId);
    } catch (Exception e) {
      log.error(
          "Failed to submit theses replacement for sourceEndpointId: {}", sourceEndpointId, e);
      if (e instanceof RuntimeException re) throw re;
      throw new RuntimeException("Submission failed", e);
    }
  }
}
