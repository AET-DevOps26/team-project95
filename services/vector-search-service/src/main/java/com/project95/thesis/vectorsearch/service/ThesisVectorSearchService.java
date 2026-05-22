package com.project95.thesis.vectorsearch.service;

import com.project95.thesis.vectorsearch.dto.VectorSearchRequest;
import com.project95.thesis.vectorsearch.dto.VectorSearchResponse;
import org.springframework.stereotype.Service;

@Service
public class ThesisVectorSearchService {

    public VectorSearchResponse semanticSearch(VectorSearchRequest request) {
        return new VectorSearchResponse();
    }
}
