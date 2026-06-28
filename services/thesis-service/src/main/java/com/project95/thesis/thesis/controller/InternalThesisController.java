package com.project95.thesis.thesis.controller;

import com.project95.thesis.management.dto.*;
import com.project95.thesis.management.dto.SourceEndpointDto;
import com.project95.thesis.thesis.domain.SourceEndpoint;
import com.project95.thesis.thesis.repository.SourceEndpointRepository;
import com.project95.thesis.thesis.service.ScrapeRunService;
import com.project95.thesis.thesis.service.ThesisCoordinationService;
import com.project95.thesis.thesis.utils.HtmlNormalizer;
import com.project95.thesis.thesis.utils.Utils;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/thesis-service")
public class InternalThesisController {

  private final ThesisCoordinationService thesisCoordinationService;
  private final ScrapeRunService scrapeRunService;
  private final SourceEndpointRepository sourceEndpointRepository;

  public InternalThesisController(
      ThesisCoordinationService thesisCoordinationService,
      ScrapeRunService scrapeRunService,
      SourceEndpointRepository sourceEndpointRepository) {
    this.thesisCoordinationService = thesisCoordinationService;
    this.scrapeRunService = scrapeRunService;
    this.sourceEndpointRepository = sourceEndpointRepository;
  }

  @GetMapping("/source-endpoints")
  public ResponseEntity<SourceEndpointListResponseDto> listSourceEndpoints() {
    List<SourceEndpoint> entities = sourceEndpointRepository.findActiveWithChairEagerly();

    List<SourceEndpointDto> dtos =
        entities.stream().map(this::mapToSourceEndpointDto).collect(Collectors.toList());

    SourceEndpointListResponseDto response = new SourceEndpointListResponseDto();
    response.setEndpoints(dtos);
    return ResponseEntity.ok(response);
  }

  private SourceEndpointDto mapToSourceEndpointDto(SourceEndpoint entity) {
    SourceEndpointDto dto = new SourceEndpointDto();
    dto.setId(entity.getId());
    dto.setChairId(entity.getChair().getId());
    dto.setChairName(entity.getChair().getName());
    if (entity.getUrl() != null) {
      dto.setUrl(URI.create(entity.getUrl()));
    }
    dto.setStatus(entity.getStatus());
    dto.setLastScrapedAt(entity.getLastScrapedAt());
    dto.setLastContentHash(entity.getLastContentHash());
    return dto;
  }

  @PostMapping("/scrape-runs")
  public ResponseEntity<ScrapeRunLogResponseDto> logScrapeRun(
      @Valid @RequestBody ScrapeRunLogRequestDto request) {
    Objects.requireNonNull(request, "request payload must not be null");
    ScrapeRunLogResponseDto response = scrapeRunService.logScrapeRun(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PutMapping("/source-endpoints/{sourceEndpointId}/theses")
  public ResponseEntity<SourceEndpointThesesReplacementResponseDto> replaceChairTheses(
      @PathVariable("sourceEndpointId") Long sourceEndpointId,
      @Valid @RequestBody SourceEndpointThesesReplacementRequestDto request) {

    Objects.requireNonNull(sourceEndpointId, "sourceEndpointId must not be null");
    Objects.requireNonNull(request, "request payload must not be null");

    SourceEndpointThesesReplacementResponseDto response =
        thesisCoordinationService.executeScrapeIngestionPipeline(sourceEndpointId, request);
    return ResponseEntity.ok(response);
  }
}
