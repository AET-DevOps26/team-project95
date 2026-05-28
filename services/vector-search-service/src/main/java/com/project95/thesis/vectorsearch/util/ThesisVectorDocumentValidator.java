package com.project95.thesis.vectorsearch.util;

import com.project95.thesis.vectorsearch.dto.VectorThesisDocumentDto;

public final class ThesisVectorDocumentValidator {

  private ThesisVectorDocumentValidator() {}

  public static void validateForChair(Long chairId, VectorThesisDocumentDto thesis) {
    if (thesis == null) {
      throw new IllegalArgumentException("Thesis document must not be null");
    }
    if (thesis.getThesisId() == null) {
      throw new IllegalArgumentException("Thesis document thesisId must not be null");
    }
    if (thesis.getChairId() == null) {
      throw new IllegalArgumentException("Thesis document chairId must not be null");
    }
    if (!chairId.equals(thesis.getChairId())) {
      throw new IllegalArgumentException("All thesis documents must belong to chairId=" + chairId);
    }
    if (ThesisVectorUtils.isBlank(thesis.getTitle())) {
      throw new IllegalArgumentException("Thesis document title must not be blank");
    }
    if (thesis.getSourceUrl() == null) {
      throw new IllegalArgumentException("Thesis document sourceUrl must not be null");
    }
  }
}
