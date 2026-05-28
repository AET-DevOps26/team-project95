package com.project95.thesis.thesis.service;

import com.project95.thesis.management.dto.ScrapeRunLogRequestDto;
import com.project95.thesis.management.dto.ScrapeRunLogResponseDto;
import com.project95.thesis.thesis.domain.ScrapeRun;
import com.project95.thesis.thesis.domain.SourceEndpoint;
import com.project95.thesis.thesis.repository.ScrapeRunRepository;
import com.project95.thesis.thesis.repository.SourceEndpointRepository;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScrapeRunService {

  private static final Logger log = LoggerFactory.getLogger(ScrapeRunService.class);

  private final ScrapeRunRepository scrapeRunRepository;
  private final SourceEndpointRepository sourceEndpointRepository;

  public ScrapeRunService(
      ScrapeRunRepository scrapeRunRepository, SourceEndpointRepository sourceEndpointRepository) {
    this.scrapeRunRepository = scrapeRunRepository;
    this.sourceEndpointRepository = sourceEndpointRepository;
  }

  @Transactional
  public ScrapeRunLogResponseDto logScrapeRun(ScrapeRunLogRequestDto request) {
    Objects.requireNonNull(request, "request must not be null");
    if (request.getSourceEndpointId() == null) {
      throw new IllegalArgumentException("sourceEndpointId must not be null");
    }
    if (request.getStartedAt() == null) {
      throw new IllegalArgumentException("startedAt must not be null");
    }
    if (request.getStatus() == null) {
      throw new IllegalArgumentException("status must not be null");
    }

    log.info("Logging scrape run for endpoint ID: {}", request.getSourceEndpointId());

    SourceEndpoint endpoint =
        sourceEndpointRepository
            .findById(request.getSourceEndpointId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Source endpoint not found with ID: " + request.getSourceEndpointId()));

    ScrapeRun run = new ScrapeRun();
    run.setSourceEndpoint(endpoint);
    run.setStartedAt(request.getStartedAt());
    run.setFinishedAt(request.getFinishedAt());
    run.setStatus(request.getStatus().toString());
    run.setErrorMessage(unwrap(request.getErrorMessage()));
    run.setCandidatesFound(
        unwrap(request.getCandidatesFound()) != null ? unwrap(request.getCandidatesFound()) : 0);

    ScrapeRun savedRun = scrapeRunRepository.save(run);

    // Update the last_scraped_at timestamp on the source endpoint
    endpoint.setLastScrapedAt(run.getFinishedAt());
    sourceEndpointRepository.save(endpoint);

    ScrapeRunLogResponseDto response = new ScrapeRunLogResponseDto();
    response.setId(savedRun.getId());
    response.setStatus(savedRun.getStatus());

    return response;
  }

  @Transactional
  public void updateScrapeRunStatus(Long id, String status, String errorMessage) {
    ScrapeRun run =
        scrapeRunRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Scrape run not found with ID: " + id));
    run.setStatus(status);
    if (errorMessage != null) {
      run.setErrorMessage(errorMessage);
    }
    scrapeRunRepository.save(run);
  }

  private <T> T unwrap(org.openapitools.jackson.nullable.JsonNullable<T> nullable) {
    return (nullable != null && nullable.isPresent()) ? nullable.get() : null;
  }
}
