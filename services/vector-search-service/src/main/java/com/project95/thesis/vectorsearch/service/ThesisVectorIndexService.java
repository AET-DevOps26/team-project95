package com.project95.thesis.vectorsearch.service;

import com.project95.thesis.vectorsearch.dto.ReplaceChairVectorsRequestDto;
import com.project95.thesis.vectorsearch.dto.ReplaceChairVectorsResponseDto;
import com.project95.thesis.vectorsearch.dto.VectorThesisDocumentDto;
import com.project95.thesis.vectorsearch.util.ThesisVectorDocumentValidator;
import com.project95.thesis.vectorsearch.util.ThesisVectorMetadata;
import com.project95.thesis.vectorsearch.util.ThesisVectorUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ThesisVectorIndexService {

  private final VectorStore vectorStore;

  public ThesisVectorIndexService(VectorStore vectorStore) {
    this.vectorStore = vectorStore;
  }

  @Transactional
  public ReplaceChairVectorsResponseDto indexChairTheses(
      Long chairId, ReplaceChairVectorsRequestDto request) {
    if (chairId == null) {
      throw new IllegalArgumentException("chairId must not be null");
    }
    if (request == null || request.getTheses() == null) {
      throw new IllegalArgumentException(
          "Replace chair vectors request and theses must not be null");
    }

    List<VectorThesisDocumentDto> theses = request.getTheses();
    List<Document> documents = new ArrayList<>(theses.size());
    for (VectorThesisDocumentDto thesis : theses) {
      ThesisVectorDocumentValidator.validateForChair(chairId, thesis);
      documents.add(toDocument(thesis));
    }

    vectorStore.delete(ThesisVectorUtils.chairFilter(chairId));

    if (!documents.isEmpty()) {
      vectorStore.add(documents);
    }

    // Spring AI VectorStore#delete(filter) does not expose the number of deleted rows.
    return new ReplaceChairVectorsResponseDto(chairId, 0, documents.size());
  }

  private Document toDocument(VectorThesisDocumentDto thesis) {
    return new Document(
        documentId(thesis.getThesisId()), buildContent(thesis), buildMetadata(thesis));
  }

  private String documentId(Long thesisId) {
    return UUID.nameUUIDFromBytes(("thesis:" + thesisId).getBytes(StandardCharsets.UTF_8))
        .toString();
  }

  private String buildContent(VectorThesisDocumentDto thesis) {
    List<String> parts = new ArrayList<>();
    ThesisVectorUtils.addIfPresent(parts, thesis.getTitle());
    ThesisVectorUtils.addIfPresent(parts, thesis.getAiOverview());
    ThesisVectorUtils.addIfPresent(parts, thesis.getOriginalDescription());
    ThesisVectorUtils.addLabelledIfPresent(parts, "Research area", thesis.getResearchArea());
    ThesisVectorUtils.addLabelledIfPresent(parts, "Degree type", thesis.getDegreeType());

    List<String> tags = normalizedTags(thesis);
    if (!tags.isEmpty()) {
      ThesisVectorUtils.addLabelledIfPresent(parts, "Tags", String.join(", ", tags));
    }

    return String.join("\n\n", parts);
  }

  private Map<String, Object> buildMetadata(VectorThesisDocumentDto thesis) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put(ThesisVectorMetadata.THESIS_ID, thesis.getThesisId());
    metadata.put(ThesisVectorMetadata.CHAIR_ID, thesis.getChairId());
    metadata.put(ThesisVectorMetadata.TITLE, thesis.getTitle());
    ThesisVectorUtils.putIfPresent(
        metadata, ThesisVectorMetadata.DEGREE_TYPE, thesis.getDegreeType());
    ThesisVectorUtils.putIfPresent(
        metadata, ThesisVectorMetadata.RESEARCH_AREA, thesis.getResearchArea());
    metadata.put(ThesisVectorMetadata.SOURCE_URL, thesis.getSourceUrl().toString());
    List<String> tags = normalizedTags(thesis);
    if (!tags.isEmpty()) {
      metadata.put(ThesisVectorMetadata.TAGS, tags);
    }
    return metadata;
  }

  private List<String> normalizedTags(VectorThesisDocumentDto thesis) {
    if (thesis.getTags() == null) {
      return List.of();
    }
    return thesis.getTags().stream()
        .filter(tag -> !ThesisVectorUtils.isBlank(tag))
        .map(String::trim)
        .toList();
  }
}
