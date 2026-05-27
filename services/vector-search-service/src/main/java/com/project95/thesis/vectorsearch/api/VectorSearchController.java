package com.project95.thesis.vectorsearch.api;

import com.project95.thesis.vectorsearch.dto.ChairThesesReplacementRequest;
import com.project95.thesis.vectorsearch.dto.ChairThesesReplacementResponse;
import com.project95.thesis.vectorsearch.dto.GenAIExtractionRequest;
import com.project95.thesis.vectorsearch.dto.GenAIExtractionResponse;
import com.project95.thesis.vectorsearch.dto.HealthResponse;
import com.project95.thesis.vectorsearch.dto.ReplaceChairVectorsRequest;
import com.project95.thesis.vectorsearch.dto.ReplaceChairVectorsResponse;
import com.project95.thesis.vectorsearch.dto.ScrapeRunLogRequest;
import com.project95.thesis.vectorsearch.dto.ScrapeRunLogResponse;
import com.project95.thesis.vectorsearch.dto.SourceEndpointListResponse;
import com.project95.thesis.vectorsearch.dto.TriggerScrapeResponse;
import com.project95.thesis.vectorsearch.dto.VectorSearchRequest;
import com.project95.thesis.vectorsearch.dto.VectorSearchResponse;
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
  public ResponseEntity<VectorSearchResponse> semanticSearch(
      VectorSearchRequest vectorSearchRequest) {
    return ResponseEntity.ok(thesisVectorSearchService.semanticSearch(vectorSearchRequest));
  }

  @Override
  public ResponseEntity<ReplaceChairVectorsResponse> indexChairTheses(
      Long chairId, ReplaceChairVectorsRequest replaceChairVectorsRequest) {
    return ResponseEntity.ok(
        thesisVectorIndexService.indexChairTheses(chairId, replaceChairVectorsRequest));
  }

  @Override
  public ResponseEntity<ScrapeRunLogResponse> logScrapeRun(
      ScrapeRunLogRequest scrapeRunLogRequest) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<HealthResponse> healthCheck() {
    return ResponseEntity.ok(new HealthResponse("UP").service("vector-search-service"));
  }

  @Override
  public ResponseEntity<GenAIExtractionResponse> extractThesesFromRawContent(
      GenAIExtractionRequest genAIExtractionRequest) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<SourceEndpointListResponse> listSourceEndpointsForScraping() {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<ChairThesesReplacementResponse> replaceChairTheses(
      Long chairId, ChairThesesReplacementRequest chairThesesReplacementRequest) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<TriggerScrapeResponse> triggerScrape() {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
}
