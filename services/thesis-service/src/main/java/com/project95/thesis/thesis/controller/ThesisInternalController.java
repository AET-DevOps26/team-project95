package com.project95.thesis.thesis.controller;

import com.project95.thesis.management.dto.DetectChangesRequestDto;
import com.project95.thesis.management.dto.DetectChangesResponseDto;
import com.project95.thesis.thesis.domain.SourceEndpoint;
import com.project95.thesis.thesis.repository.SourceEndpointRepository;
import com.project95.thesis.thesis.utils.HtmlNormalizer;
import com.project95.thesis.thesis.utils.Utils;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ThesisInternalController {

  private final SourceEndpointRepository sourceEndpointRepository;

  public ThesisInternalController(SourceEndpointRepository sourceEndpointRepository) {
    this.sourceEndpointRepository = sourceEndpointRepository;
  }

  @PostMapping("/thesis-internal/v1/source-endpoints/{sourceEndpointId}/detect-changes")
  public ResponseEntity<DetectChangesResponseDto> detectChanges(
      @PathVariable("sourceEndpointId") Long sourceEndpointId,
      @Valid @RequestBody DetectChangesRequestDto request) {

    Objects.requireNonNull(sourceEndpointId, "sourceEndpointId must not be null");
    Objects.requireNonNull(request, "request payload must not be null");

    SourceEndpoint endpoint =
        sourceEndpointRepository
            .findById(sourceEndpointId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Source endpoint not found with ID: " + sourceEndpointId));

    String sanitizedHtml = HtmlNormalizer.sanitizeHtml(request.getRawHtml());
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
