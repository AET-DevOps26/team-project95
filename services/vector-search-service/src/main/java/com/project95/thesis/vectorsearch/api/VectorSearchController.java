package com.project95.thesis.vectorsearch.api;

import com.project95.thesis.vectorsearch.dto.HealthResponseDto;
import com.project95.thesis.vectorsearch.dto.ReplaceSourceEndpointVectorsRequestDto;
import com.project95.thesis.vectorsearch.dto.ReplaceSourceEndpointVectorsResponseDto;
import com.project95.thesis.vectorsearch.dto.VectorSearchRequestDto;
import com.project95.thesis.vectorsearch.dto.VectorSearchResponseDto;
import com.project95.thesis.vectorsearch.service.ThesisVectorIndexService;
import com.project95.thesis.vectorsearch.service.ThesisVectorSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VectorSearchController implements VectorSearchApiApi, HealthApi {

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
  public ResponseEntity<ReplaceSourceEndpointVectorsResponseDto> indexSourceEndpointTheses(
      Long sourceEndpointId,
      ReplaceSourceEndpointVectorsRequestDto replaceSourceEndpointVectorsRequest) {
    return ResponseEntity.ok(
        thesisVectorIndexService.indexSourceEndpointTheses(
            sourceEndpointId, replaceSourceEndpointVectorsRequest));
  }

  @Override
  public ResponseEntity<HealthResponseDto> healthCheck() {
    return ResponseEntity.ok(new HealthResponseDto("UP").service("vector-search-service"));
  }
}
