package com.project95.thesis.thesis.controller;

import com.project95.thesis.management.dto.ChairThesesReplacementRequest;
import com.project95.thesis.management.dto.ChairThesesReplacementResponse;
import com.project95.thesis.management.dto.ScrapeRunLogRequest;
import com.project95.thesis.management.dto.ScrapeRunLogResponse;
import com.project95.thesis.thesis.service.ScrapeRunService;
import com.project95.thesis.thesis.service.ThesisCoordinationService;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/thesis-service")
public class ThesisFrontendController {

  private final ThesisCoordinationService thesisCoordinationService;
  private final ScrapeRunService scrapeRunService;

  public ThesisFrontendController(
      ThesisCoordinationService thesisCoordinationService, ScrapeRunService scrapeRunService) {
    this.thesisCoordinationService = thesisCoordinationService;
    this.scrapeRunService = scrapeRunService;
  }

  @PostMapping("/scrape-runs")
  public ResponseEntity<ScrapeRunLogResponse> logScrapeRun(
      @RequestBody ScrapeRunLogRequest request) {
    Objects.requireNonNull(request, "request payload must not be null");
    ScrapeRunLogResponse response = scrapeRunService.logScrapeRun(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PutMapping("/chairs/{chairId}/theses")
  public ResponseEntity<ChairThesesReplacementResponse> replaceChairTheses(
      @PathVariable("chairId") Long chairId, @RequestBody ChairThesesReplacementRequest request) {

    Objects.requireNonNull(chairId, "chairId must not be null");
    Objects.requireNonNull(request, "request payload must not be null");

    ChairThesesReplacementResponse response =
        thesisCoordinationService.executeScrapeIngestionPipeline(chairId, request);
    return ResponseEntity.ok(response);
  }
}
