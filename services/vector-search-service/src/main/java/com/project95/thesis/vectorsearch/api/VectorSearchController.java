package com.project95.thesis.vectorsearch.api;

import com.project95.thesis.vectorsearch.dto.ChairThesesReplacementRequestDto;
import com.project95.thesis.vectorsearch.dto.ChairThesesReplacementResponseDto;
import com.project95.thesis.vectorsearch.dto.GenAIExtractionRequestDto;
import com.project95.thesis.vectorsearch.dto.GenAIExtractionResponseDto;
import com.project95.thesis.vectorsearch.dto.HealthResponseDto;
import com.project95.thesis.vectorsearch.dto.ReplaceChairVectorsRequestDto;
import com.project95.thesis.vectorsearch.dto.ReplaceChairVectorsResponseDto;
import com.project95.thesis.vectorsearch.dto.ScrapeRunLogRequestDto;
import com.project95.thesis.vectorsearch.dto.ScrapeRunLogResponseDto;
import com.project95.thesis.vectorsearch.dto.SourceEndpointListResponseDto;
import com.project95.thesis.vectorsearch.dto.TriggerScrapeResponseDto;
import com.project95.thesis.vectorsearch.dto.VectorSearchRequestDto;
import com.project95.thesis.vectorsearch.dto.VectorSearchResponseDto;
import com.project95.thesis.vectorsearch.service.ThesisVectorIndexService;
import com.project95.thesis.vectorsearch.service.ThesisVectorSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VectorSearchController implements InternalApi {

  private final ThesisVectorSearchService thesisVectorSearchService;
  private final ThesisVectorIndexService thesisVectorIndexService;

  public VectorSearchController(
      ThesisVectorSearchService thesisVectorSearchService,
      ThesisVectorIndexService thesisVectorIndexService) {
    this.thesisVectorSearchService = thesisVectorSearchService;
    this.thesisVectorIndexService = thesisVectorIndexService;
  }

  @Override
  public ResponseEntity<VectorSearchResponseDto> semanticSearch(
      VectorSearchRequestDto vectorSearchRequest) {
    return ResponseEntity.ok(thesisVectorSearchService.semanticSearch(vectorSearchRequest));
  }

  @Override
  public ResponseEntity<ReplaceChairVectorsResponseDto> indexChairTheses(
      Long chairId, ReplaceChairVectorsRequestDto replaceChairVectorsRequest) {
    return ResponseEntity.ok(
        thesisVectorIndexService.indexChairTheses(chairId, replaceChairVectorsRequest));
  }

  @Override
  public ResponseEntity<ScrapeRunLogResponseDto> logScrapeRun(
      ScrapeRunLogRequestDto scrapeRunLogRequest) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<HealthResponseDto> healthCheck() {
    return ResponseEntity.ok(new HealthResponseDto("UP").service("vector-search-service"));
  }

  @Override
  public ResponseEntity<GenAIExtractionResponseDto> extractThesesFromRawContent(
      GenAIExtractionRequestDto genAIExtractionRequest) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<SourceEndpointListResponseDto> listSourceEndpointsForScraping() {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<ChairThesesReplacementResponseDto> replaceChairTheses(
      Long chairId, ChairThesesReplacementRequestDto chairThesesReplacementRequest) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<TriggerScrapeResponseDto> triggerScrape() {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
}
