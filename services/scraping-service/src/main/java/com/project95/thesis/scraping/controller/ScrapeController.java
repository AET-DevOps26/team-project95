package com.project95.thesis.scraping.controller;

import com.project95.thesis.scraping.dto.TriggerScrapeResponseDto;
import com.project95.thesis.scraping.service.ScrapeCoordinationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/scraping-service")
public class ScrapeController {

  private final ScrapeCoordinationService scrapeCoordinationService;
  private final TaskExecutor taskExecutor;

  public ScrapeController(
      ScrapeCoordinationService scrapeCoordinationService,
      @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
    this.scrapeCoordinationService = scrapeCoordinationService;
    this.taskExecutor = taskExecutor;
  }

  @PostMapping(value = "/scrape", produces = "application/json")
  public ResponseEntity<TriggerScrapeResponseDto> triggerScrape() {
    taskExecutor.execute(scrapeCoordinationService::runScrapeCycle);

    TriggerScrapeResponseDto response =
        new TriggerScrapeResponseDto(true).message("Scrape run started.");
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
  }
}
