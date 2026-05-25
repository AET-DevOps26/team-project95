package com.project95.thesis.thesis.controller;

import com.project95.thesis.management.dto.ChairThesesReplacementRequest;
import com.project95.thesis.management.dto.ChairThesesReplacementResponse;
import com.project95.thesis.thesis.service.ThesisCoordinationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/internal/v1/thesis-service")
public class ThesisFrontendController {

  private final ThesisCoordinationService thesisCoordinationService;

  public ThesisFrontendController(ThesisCoordinationService thesisCoordinationService) {
    this.thesisCoordinationService = thesisCoordinationService;
  }

  @PutMapping("/chairs/{chairId}/theses")
  public ResponseEntity<ChairThesesReplacementResponse> replaceChairTheses(
    @PathVariable("chairId") Long chairId,
    @RequestBody ChairThesesReplacementRequest request) {
        
    Objects.requireNonNull(chairId, "chairId must not be null");
    Objects.requireNonNull(request, "request payload must not be null");

    ChairThesesReplacementResponse response = thesisCoordinationService.executeScrapeIngestionPipeline(chairId, request);
    return ResponseEntity.ok(response);
  }
}
