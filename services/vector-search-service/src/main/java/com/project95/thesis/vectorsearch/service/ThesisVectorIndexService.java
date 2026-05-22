package com.project95.thesis.vectorsearch.service;

import com.project95.thesis.vectorsearch.dto.ReplaceChairVectorsRequest;
import com.project95.thesis.vectorsearch.dto.ReplaceChairVectorsResponse;
import com.project95.thesis.vectorsearch.dto.VectorThesisDocument;
import org.springframework.stereotype.Service;

@Service
public class ThesisVectorIndexService {

    public ReplaceChairVectorsResponse indexChairTheses(Long chairId, ReplaceChairVectorsRequest request) {
        int inserted = 0;

        if (request.getTheses() != null) {
            for (VectorThesisDocument thesis : request.getTheses()) {
                if (thesis.getChairId() != null && !chairId.equals(thesis.getChairId())) {
                    throw new IllegalArgumentException("All thesis documents must belong to chairId=" + chairId);
                }
                inserted++;
            }
        }

        return new ReplaceChairVectorsResponse(chairId, 0, inserted);
    }
}
