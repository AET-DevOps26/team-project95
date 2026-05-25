package com.project95.thesis.vectorsearch.service;

import com.project95.thesis.vectorsearch.dto.ReplaceChairVectorsRequest;
import com.project95.thesis.vectorsearch.dto.ReplaceChairVectorsResponse;
import com.project95.thesis.vectorsearch.dto.VectorThesisDocument;
import com.project95.thesis.vectorsearch.util.ThesisVectorDocumentValidator;
import com.project95.thesis.vectorsearch.util.ThesisVectorMetadata;
import com.project95.thesis.vectorsearch.util.ThesisVectorUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class ThesisVectorIndexService {

    private final VectorStore vectorStore;

    public ThesisVectorIndexService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public ReplaceChairVectorsResponse indexChairTheses(Long chairId, ReplaceChairVectorsRequest request) {
        List<VectorThesisDocument> theses = request == null || request.getTheses() == null
                ? List.of()
                : request.getTheses();

        List<Document> documents = new ArrayList<>(theses.size());
        for (VectorThesisDocument thesis : theses) {
            ThesisVectorDocumentValidator.validateForChair(chairId, thesis);
            documents.add(toDocument(thesis));
        }

        vectorStore.delete(ThesisVectorUtils.chairFilter(chairId));

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }

        // Spring AI VectorStore#delete(filter) does not expose the number of deleted rows.
        return new ReplaceChairVectorsResponse(chairId, 0, documents.size());
    }

    private Document toDocument(VectorThesisDocument thesis) {
        return new Document(
                documentId(thesis.getThesisId()),
                buildContent(thesis),
                buildMetadata(thesis)
        );
    }

    private String documentId(Long thesisId) {
        return UUID.nameUUIDFromBytes(("thesis:" + thesisId).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String buildContent(VectorThesisDocument thesis) {
        List<String> parts = new ArrayList<>();
        ThesisVectorUtils.addIfPresent(parts, thesis.getTitle());
        ThesisVectorUtils.addIfPresent(parts, ThesisVectorUtils.nullableValue(thesis.getAiOverview()));
        ThesisVectorUtils.addIfPresent(parts, ThesisVectorUtils.nullableValue(thesis.getOriginalDescription()));
        ThesisVectorUtils.addLabelledIfPresent(parts, "Research area", ThesisVectorUtils.nullableValue(thesis.getResearchArea()));
        ThesisVectorUtils.addLabelledIfPresent(parts, "Degree type", ThesisVectorUtils.nullableValue(thesis.getDegreeType()));

        if (thesis.getTags() != null && !thesis.getTags().isEmpty()) {
            String tags = String.join(", ", thesis.getTags().stream().filter(tag -> !ThesisVectorUtils.isBlank(tag)).toList());
            ThesisVectorUtils.addLabelledIfPresent(parts, "Tags", tags);
        }

        return String.join("\n\n", parts);
    }

    private Map<String, Object> buildMetadata(VectorThesisDocument thesis) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(ThesisVectorMetadata.THESIS_ID, thesis.getThesisId());
        metadata.put(ThesisVectorMetadata.CHAIR_ID, thesis.getChairId());
        metadata.put(ThesisVectorMetadata.TITLE, thesis.getTitle());
        ThesisVectorUtils.putIfPresent(metadata, ThesisVectorMetadata.DEGREE_TYPE, ThesisVectorUtils.nullableValue(thesis.getDegreeType()));
        ThesisVectorUtils.putIfPresent(metadata, ThesisVectorMetadata.RESEARCH_AREA, ThesisVectorUtils.nullableValue(thesis.getResearchArea()));
        metadata.put(ThesisVectorMetadata.SOURCE_URL, thesis.getSourceUrl().toString());
        if (thesis.getTags() != null && !thesis.getTags().isEmpty()) {
            metadata.put(ThesisVectorMetadata.TAGS, thesis.getTags());
        }
        return metadata;
    }

}
