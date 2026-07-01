package com.project95.thesis.thesis.controller;

import com.project95.thesis.management.api.ThesisServiceInternalApiApi;
import com.project95.thesis.management.dto.*;
import com.project95.thesis.thesis.domain.SourceEndpoint;
import com.project95.thesis.thesis.repository.SourceEndpointRepository;
import com.project95.thesis.thesis.service.ScrapeRunService;
import com.project95.thesis.thesis.service.ThesisCoordinationService;
import com.project95.thesis.thesis.utils.HtmlNormalizer;
import com.project95.thesis.thesis.utils.Utils;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class InternalThesisController implements ThesisServiceInternalApiApi {

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

  @Override
  public ResponseEntity<SourceEndpointListResponseDto> listSourceEndpointsForScraping() {
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

  @Override
  public ResponseEntity<ScrapeRunLogResponseDto> logScrapeRun(
      ScrapeRunLogRequestDto scrapeRunLogRequestDto) {
    Objects.requireNonNull(scrapeRunLogRequestDto, "request payload must not be null");
    ScrapeRunLogResponseDto response = scrapeRunService.logScrapeRun(scrapeRunLogRequestDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  public ResponseEntity<SourceEndpointThesesReplacementResponseDto> replaceChairTheses(
      Long sourceEndpointId,
      SourceEndpointThesesReplacementRequestDto sourceEndpointThesesReplacementRequestDto) {
    Objects.requireNonNull(sourceEndpointId, "sourceEndpointId must not be null");
    Objects.requireNonNull(
        sourceEndpointThesesReplacementRequestDto, "request payload must not be null");

    SourceEndpointThesesReplacementResponseDto response =
        thesisCoordinationService.executeScrapeIngestionPipeline(
            sourceEndpointId, sourceEndpointThesesReplacementRequestDto);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<DetectChangesResponseDto> detectChanges(
      Long sourceEndpointId, DetectChangesRequestDto detectChangesRequestDto) {
    Objects.requireNonNull(sourceEndpointId, "sourceEndpointId must not be null");
    Objects.requireNonNull(detectChangesRequestDto, "request payload must not be null");

    SourceEndpoint endpoint =
        sourceEndpointRepository
            .findById(sourceEndpointId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Source endpoint not found with ID: " + sourceEndpointId));

    String sanitizedHtml =
        HtmlNormalizer.sanitizeHtml(detectChangesRequestDto.getRawHtml(), endpoint.getUrl());
    String normalizedText = HtmlNormalizer.getNormalizedText(sanitizedHtml);
    String newHash = Utils.sha256(normalizedText);
    String oldHash = endpoint.getLastContentHash();

    boolean changed = (oldHash == null) || !oldHash.equals(newHash);

    DetectChangesResponseDto response = new DetectChangesResponseDto();
    response.setChanged(changed);
    response.setSanitizedHtml(sanitizedHtml);
    response.setContentHash(newHash);

    return ResponseEntity.ok(response);
  }
}
