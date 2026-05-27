package com.project95.thesis.vectorsearch.util;

import java.util.List;
import java.util.Map;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.ai.vectorstore.filter.Filter.ExpressionType;
import org.springframework.ai.vectorstore.filter.Filter.Key;
import org.springframework.ai.vectorstore.filter.Filter.Value;

public final class ThesisVectorUtils {

  private ThesisVectorUtils() {}

  public static Expression chairFilter(Long chairId) {
    return new Expression(
        ExpressionType.EQ, new Key(ThesisVectorMetadata.CHAIR_ID), new Value(chairId));
  }

  public static void putIfPresent(Map<String, Object> metadata, String key, String value) {
    if (!isBlank(value)) {
      metadata.put(key, value.trim());
    }
  }

  public static void addLabelledIfPresent(List<String> parts, String label, String value) {
    if (!isBlank(value)) {
      parts.add(label + ": " + value.trim());
    }
  }

  public static void addIfPresent(List<String> parts, String value) {
    if (!isBlank(value)) {
      parts.add(value.trim());
    }
  }

  public static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
