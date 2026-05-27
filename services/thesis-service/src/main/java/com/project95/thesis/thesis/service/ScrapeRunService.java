package com.project95.thesis.thesis.service;

import com.project95.thesis.management.dto.ScrapeRunLogRequest;
import com.project95.thesis.management.dto.ScrapeRunLogResponse;
import com.project95.thesis.thesis.domain.ScrapeRun;
import com.project95.thesis.thesis.domain.SourceEndpoint;
import com.project95.thesis.thesis.repository.ScrapeRunRepository;
import com.project95.thesis.thesis.repository.SourceEndpointRepository;
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
  public ScrapeRunLogResponse logScrapeRun(ScrapeRunLogRequest request) {
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

    ScrapeRunLogResponse response = new ScrapeRunLogResponse();
    response.setId(savedRun.getId());
    response.setStatus(savedRun.getStatus());

    return response;
  }

  private <T> T unwrap(org.openapitools.jackson.nullable.JsonNullable<T> nullable) {
    return (nullable != null && nullable.isPresent()) ? nullable.get() : null;
  }
}
