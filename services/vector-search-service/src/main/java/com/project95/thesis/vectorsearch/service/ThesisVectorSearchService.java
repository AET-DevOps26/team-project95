package com.project95.thesis.vectorsearch.service;

import com.project95.thesis.vectorsearch.dto.VectorSearchFilters;
import com.project95.thesis.vectorsearch.dto.VectorSearchRequest;
import com.project95.thesis.vectorsearch.dto.VectorSearchResponse;
import com.project95.thesis.vectorsearch.dto.VectorSearchResult;
import com.project95.thesis.vectorsearch.util.ThesisVectorMetadata;
import com.project95.thesis.vectorsearch.util.ThesisVectorUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final VectorStore vectorStore;

    public ThesisVectorSearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public VectorSearchResponse semanticSearch(VectorSearchRequest request) {
        if (request == null || ThesisVectorUtils.isBlank(request.getQuery())) {
            throw new IllegalArgumentException("Search query must not be blank");
        }

        SearchRequest.Builder searchRequest = SearchRequest.builder()
                .query(request.getQuery().trim())
                .topK(resolveLimit(request.getLimit()));

        Expression filterExpression = buildFilterExpression(request.getFilters());
        if (filterExpression != null) {
            searchRequest.filterExpression(filterExpression);
        }

        List<Document> documents = vectorStore.similaritySearch(searchRequest.build());
        List<VectorSearchResult> results = documents == null
                ? List.of()
                : documents.stream().map(this::toResult).toList();

        return new VectorSearchResponse(results);
    }

    private int resolveLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(MAX_LIMIT, limit));
    }

    private Expression buildFilterExpression(VectorSearchFilters filters) {
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
        List<?> filteredValues = values == null
                ? List.of()
                : values.stream()
                        .filter(value -> value != null && !ThesisVectorUtils.isBlank(String.valueOf(value)))
                        .toList();

        if (!filteredValues.isEmpty()) {
            expressions.add(new Expression(ExpressionType.IN, new Key(metadataKey), new Value(filteredValues)));
        }
    }

    private VectorSearchResult toResult(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        Long thesisId = toLong(metadata.get(ThesisVectorMetadata.THESIS_ID));
        Long chairId = toLong(metadata.get(ThesisVectorMetadata.CHAIR_ID));
        Float score = document.getScore() == null ? 0.0F : document.getScore().floatValue();

        return new VectorSearchResult(thesisId, score).chairId(chairId);
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
