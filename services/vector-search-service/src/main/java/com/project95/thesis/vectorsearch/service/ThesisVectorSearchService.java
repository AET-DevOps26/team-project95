package com.project95.thesis.vectorsearch.service;

import com.project95.thesis.vectorsearch.dto.VectorSearchFiltersDto;
import com.project95.thesis.vectorsearch.dto.VectorSearchRequestDto;
import com.project95.thesis.vectorsearch.dto.VectorSearchResponseDto;
import com.project95.thesis.vectorsearch.dto.VectorSearchResultDto;
import com.project95.thesis.vectorsearch.util.ThesisVectorMetadata;
import com.project95.thesis.vectorsearch.util.ThesisVectorUtils;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.ai.vectorstore.filter.Filter.ExpressionType;
import org.springframework.ai.vectorstore.filter.Filter.Key;
import org.springframework.ai.vectorstore.filter.Filter.Value;
import org.springframework.stereotype.Service;

@Service
public class ThesisVectorSearchService {

  private static final Logger log = LoggerFactory.getLogger(ThesisVectorSearchService.class);

  private static final int DEFAULT_LIMIT = 50;
  private static final int MAX_LIMIT = 200;

  private final VectorStore vectorStore;
  private final MeterRegistry meterRegistry;

  public ThesisVectorSearchService(VectorStore vectorStore, MeterRegistry meterRegistry) {
    this.vectorStore = vectorStore;
    this.meterRegistry = meterRegistry;
  }

  public VectorSearchResponseDto semanticSearch(VectorSearchRequestDto request) {
    if (request == null || ThesisVectorUtils.isBlank(request.getQuery())) {
      log.warn("Rejected semantic vector search request with blank query");
      throw new IllegalArgumentException("Search query must not be blank");
    }

    String normalizedQuery = request.getQuery().trim();
    int resolvedLimit = resolveLimit(request.getLimit());
    log.info(
        "Starting semantic vector search. queryLength={}, requestedLimit={}, resolvedLimit={},"
            + " filters={}",
        normalizedQuery.length(),
        request.getLimit(),
        resolvedLimit,
        filterSummary(request.getFilters()));

    SearchRequest.Builder searchRequest =
        SearchRequest.builder().query(normalizedQuery).topK(resolvedLimit);

    Expression filterExpression = buildFilterExpression(request.getFilters());
    if (filterExpression != null) {
      searchRequest.filterExpression(filterExpression);
    }

    List<Document> documents;
    long start = System.nanoTime();
    try {
      documents = vectorStore.similaritySearch(searchRequest.build());
    } finally {
      long duration = System.nanoTime() - start;
      meterRegistry
          .timer(
              "vector_search_duration_seconds",
              "query_length_range",
              getQueryLengthRange(normalizedQuery.length()))
          .record(duration, java.util.concurrent.TimeUnit.NANOSECONDS);
    }
    List<VectorSearchResultDto> results =
        documents == null ? List.of() : documents.stream().map(this::toResult).toList();
    log.info("Completed semantic vector search. resultCount={}", results.size());

    VectorSearchResponseDto response = new VectorSearchResponseDto();
    response.setResults(results);
    return response;
  }

  private String getQueryLengthRange(int length) {
    if (length < 20) {
      return "short";
    }
    if (length < 100) {
      return "medium";
    }
    return "long";
  }

  private int resolveLimit(Integer limit) {
    if (limit == null) {
      return DEFAULT_LIMIT;
    }
    return Math.max(1, Math.min(MAX_LIMIT, limit));
  }

  private String filterSummary(VectorSearchFiltersDto filters) {
    if (filters == null) {
      return "none";
    }
    return "chairIds="
        + sizeOf(filters.getChairIds())
        + ", degreeTypes="
        + sizeOf(filters.getDegreeTypes())
        + ", researchAreas="
        + sizeOf(filters.getResearchAreas());
  }

  private int sizeOf(List<?> values) {
    return values == null ? 0 : values.size();
  }

  private Expression buildFilterExpression(VectorSearchFiltersDto filters) {
    if (filters == null) {
      return null;
    }

    List<Expression> expressions = new ArrayList<>();
    addInFilter(expressions, ThesisVectorMetadata.CHAIR_ID, filters.getChairIds());
    addInFilter(expressions, ThesisVectorMetadata.DEGREE_TYPE, filters.getDegreeTypes());
    addInFilter(expressions, ThesisVectorMetadata.RESEARCH_AREA, filters.getResearchAreas());

    if (expressions.isEmpty()) {
      return null;
    }

    Expression combined = expressions.getFirst();
    for (int i = 1; i < expressions.size(); i++) {
      combined = new Expression(ExpressionType.AND, combined, expressions.get(i));
    }
    return combined;
  }

  private void addInFilter(List<Expression> expressions, String metadataKey, List<?> values) {
    List<?> filteredValues =
        values == null
            ? List.of()
            : values.stream()
                .filter(value -> value != null && !ThesisVectorUtils.isBlank(String.valueOf(value)))
                .map(this::normalizeFilterValue)
                .toList();

    if (!filteredValues.isEmpty()) {
      expressions.add(
          new Expression(ExpressionType.IN, new Key(metadataKey), new Value(filteredValues)));
    }
  }

  private Object normalizeFilterValue(Object value) {
    return value instanceof String text ? text.trim() : value;
  }

  private VectorSearchResultDto toResult(Document document) {
    Map<String, Object> metadata = document.getMetadata();
    Long thesisId = toLong(metadata.get(ThesisVectorMetadata.THESIS_ID));
    Long chairId = toLong(metadata.get(ThesisVectorMetadata.CHAIR_ID));
    Float score = document.getScore() == null ? 0.0F : document.getScore().floatValue();

    VectorSearchResultDto result = new VectorSearchResultDto();
    result.setThesisId(thesisId);
    result.setChairId(chairId);
    result.setScore(score);
    return result;
  }

  private Long toLong(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    return Long.valueOf(String.valueOf(value));
  }
}
