package com.project95.thesis.scraping.service;

import com.project95.thesis.scraping.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.openapitools.jackson.nullable.JsonNullable;
import java.util.List;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;

@Service
public class ScrapeCoordinationService {

    private static final Logger log = LoggerFactory.getLogger(ScrapeCoordinationService.class);

    private final RestClient restClient;
    private final String mainThesisServiceUrl;
    private final String genAiServiceUrl;

    public ScrapeCoordinationService(
            RestClient restClient,
            @Value("${app.services.main-thesis}") String mainThesisServiceUrl,
            @Value("${app.services.genai}") String genAiServiceUrl) {
        this.restClient = restClient;
        this.mainThesisServiceUrl = mainThesisServiceUrl;
        this.genAiServiceUrl = genAiServiceUrl;
    }

    @Scheduled(cron = "${app.scheduling.scrape-cron}")
    public void runScrapeCycle() {
        log.info("Starting scrape cycle...");

        String endpointsUrl = mainThesisServiceUrl + "/internal/v1/thesis-service/source-endpoints";
        SourceEndpointListResponse response = null;
        try {
            response = restClient.get()
                    .uri(endpointsUrl)
                    .retrieve()
                    .body(SourceEndpointListResponse.class);
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
            String rawHtml = restClient.get()
                    .uri(endpoint.getUrl())
                    .retrieve()
                    .body(String.class);

            if (rawHtml == null || rawHtml.isBlank()) {
                throw new RuntimeException("Received empty HTML from source URL");
            }

            GenAIExtractionRequest genAiRequest = new GenAIExtractionRequest();
            genAiRequest.setSourceEndpointId(endpoint.getId());
            genAiRequest.setChairId(endpoint.getChairId());
            genAiRequest.setChairName(JsonNullable.of(endpoint.getChairName()));
            genAiRequest.setSourceUrl(endpoint.getUrl());
            genAiRequest.setRawHtml(rawHtml);
            
            String extractUrl = genAiServiceUrl + "/internal/v1/genai-service/extract-theses";
            
            genAiResponse = restClient.post()
                    .uri(extractUrl)
                    .body(genAiRequest)
                    .retrieve()
                    .body(GenAIExtractionResponse.class);

        } catch (Exception e) {
            log.error("Failed to process endpoint ID {}. Skipping thesis replacement to avoid deleting existing data.", endpoint.getId(), e);
            return;
        }

        OffsetDateTime finishedAt = OffsetDateTime.now(ZoneOffset.UTC);

        ChairThesesReplacementRequest requestBody = new ChairThesesReplacementRequest();
        requestBody.setSourceEndpointId(endpoint.getId());
        requestBody.setStartedAt(startedAt);
        requestBody.setFinishedAt(finishedAt);
        
        requestBody.setStatus(ChairThesesReplacementRequest.StatusEnum.SUCCESS);
        requestBody.setErrorMessage(JsonNullable.of(null));
        requestBody.setRawHtmlSnapshotUrl(JsonNullable.of(null));

        List<ThesisProposalInput> extractedTheses = (genAiResponse != null && genAiResponse.getTheses() != null) 
                ? genAiResponse.getTheses() 
                : Collections.emptyList();
        requestBody.setTheses(extractedTheses);

        submitScrapeRun(endpoint.getChairId(), requestBody);
    }

    private void submitScrapeRun(Long chairId, ChairThesesReplacementRequest submission) {
        String submitUrl = mainThesisServiceUrl + "/internal/v1/thesis-service/chairs/" + chairId + "/theses";
        try {
            restClient.put()
                    .uri(submitUrl)
                    .body(submission)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully submitted scrape run for chairId: {}", chairId);
        } catch (Exception e) {
            log.error("Failed to submit scrape run to Main Thesis Service for sourceEndpointId: {}", chairId, e);
        }
    }
}
