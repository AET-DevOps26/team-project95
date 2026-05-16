package com.project95.thesis.ingestion_service.service;

import com.project95.thesis.ingestion_service.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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
        log.info("Starting scheduled scrape cycle...");

        String endpointsUrl = mainThesisServiceUrl + "/internal/source-endpoints";
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

        if (response == null || response.endpoints() == null || response.endpoints().isEmpty()) {
            log.info("No source endpoints found to scrape.");
            return;
        }

        for (SourceEndpoint endpoint : response.endpoints()) {
            processEndpoint(endpoint);
        }

        log.info("Scrape cycle completed.");
    }

    private void processEndpoint(SourceEndpoint endpoint) {
        log.info("Scraping endpoint: {} (Chair: {})", endpoint.url(), endpoint.chairName());
        
        OffsetDateTime startedAt = OffsetDateTime.now(ZoneOffset.UTC);
        ScrapeStatus status = ScrapeStatus.SUCCESS;
        String errorMessage = null;
        GenAIExtractionResponse genAiResponse = null;

        try {
            String rawHtml = restClient.get()
                    .uri(endpoint.url())
                    .retrieve()
                    .body(String.class);

            if (rawHtml == null || rawHtml.isBlank()) {
                throw new RuntimeException("Received empty HTML from source URL");
            }

            GenAIExtractionRequest genAiRequest = new GenAIExtractionRequest(
                    endpoint.id(),
                    endpoint.chairId(),
                    endpoint.chairName(),
                    endpoint.url(),
                    rawHtml,
                    null
            );
            
            String extractUrl = genAiServiceUrl + "/internal/genai/extract-theses";
            
            genAiResponse = restClient.post()
                    .uri(extractUrl)
                    .body(genAiRequest)
                    .retrieve()
                    .body(GenAIExtractionResponse.class);

        } catch (Exception e) {
            log.error("Failed to process endpoint ID {}", endpoint.id(), e);
            status = ScrapeStatus.FAILED;
            errorMessage = e.getMessage();
        }

        OffsetDateTime finishedAt = OffsetDateTime.now(ZoneOffset.UTC);

        ScrapeRunSubmission submission = new ScrapeRunSubmission(
                endpoint.id(),
                endpoint.chairId(),
                startedAt,
                finishedAt,
                status,
                errorMessage,
                null,
                genAiResponse != null && genAiResponse.theses() != null ? genAiResponse.theses() : Collections.emptyList()
        );

        submitScrapeRun(submission);
    }

    private void submitScrapeRun(ScrapeRunSubmission submission) {
        String submitUrl = mainThesisServiceUrl + "/internal/scrape-runs";
        try {
            restClient.post()
                    .uri(submitUrl)
                    .body(submission)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully submitted scrape run for sourceEndpointId: {}", submission.sourceEndpointId());
        } catch (Exception e) {
            log.error("Failed to submit scrape run to Main Thesis Service for sourceEndpointId: {}", submission.sourceEndpointId(), e);
        }
    }
}
