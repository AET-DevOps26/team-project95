package com.project95.thesis.vectorsearch.service;

import com.project95.thesis.vectorsearch.dto.ReplaceSourceEndpointVectorsRequestDto;
import com.project95.thesis.vectorsearch.dto.ReplaceSourceEndpointVectorsResponseDto;
import com.project95.thesis.vectorsearch.dto.VectorThesisDocumentDto;
import com.project95.thesis.vectorsearch.util.ThesisVectorMetadata;
import com.project95.thesis.vectorsearch.util.ThesisVectorUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ThesisVectorIndexService {

  private static final Logger log = LoggerFactory.getLogger(ThesisVectorIndexService.class);

  private final VectorStore vectorStore;

  public ThesisVectorIndexService(VectorStore vectorStore) {
    this.vectorStore = vectorStore;
  }

  @Transactional
  public ReplaceSourceEndpointVectorsResponseDto indexSourceEndpointTheses(
      Long sourceEndpointId, ReplaceSourceEndpointVectorsRequestDto request) {
    if (sourceEndpointId == null) {
      throw new IllegalArgumentException("sourceEndpointId must not be null");
    }
    if (request == null || request.getTheses() == null) {
      throw new IllegalArgumentException(
          "Replace source endpoint vectors request and theses must not be null");
    }

    List<VectorThesisDocumentDto> theses = request.getTheses();
    log.info(
        "Starting vector index replacement for sourceEndpointId={}. scrapeRunId={}, thesisCount={}",
        sourceEndpointId,
        request.getScrapeRunId(),
        theses.size());

    List<Document> documents = new ArrayList<>(theses.size());
    for (VectorThesisDocumentDto thesis : theses) {
      documents.add(toDocument(thesis));
    }

    log.info("Deleting existing vector documents for sourceEndpointId={}", sourceEndpointId);
    vectorStore.delete(ThesisVectorUtils.sourceEndpointFilter(sourceEndpointId));

    if (!documents.isEmpty()) {
      vectorStore.add(documents);
      log.info(
          "Inserted replacement vector documents for sourceEndpointId={}. insertedCount={}",
          sourceEndpointId,
          documents.size());
    } else {
      log.info(
          "No replacement vector documents to insert for sourceEndpointId={}", sourceEndpointId);
    }

    // Spring AI VectorStore#delete(filter) does not expose the number of deleted rows.
    ReplaceSourceEndpointVectorsResponseDto response =
        new ReplaceSourceEndpointVectorsResponseDto(sourceEndpointId, 0, documents.size());
    log.info(
        "Completed vector index replacement for sourceEndpointId={}. deletedCountUnavailable=true,"
            + " insertedCount={}",
        sourceEndpointId,
        documents.size());
    return response;
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

    return String.join("\n\n", parts);
  }

  private Map<String, Object> buildMetadata(VectorThesisDocumentDto thesis) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put(ThesisVectorMetadata.THESIS_ID, thesis.getThesisId());
    metadata.put(ThesisVectorMetadata.CHAIR_ID, thesis.getChairId());
    metadata.put(ThesisVectorMetadata.SOURCE_ENDPOINT_ID, thesis.getSourceEndpointId());
    metadata.put(ThesisVectorMetadata.TITLE, thesis.getTitle());
    ThesisVectorUtils.putIfPresent(
        metadata, ThesisVectorMetadata.DEGREE_TYPE, thesis.getDegreeType());
    ThesisVectorUtils.putIfPresent(
        metadata, ThesisVectorMetadata.RESEARCH_AREA, thesis.getResearchArea());
    metadata.put(ThesisVectorMetadata.SOURCE_URL, thesis.getSourceUrl().toString());
    return metadata;
  }
}
