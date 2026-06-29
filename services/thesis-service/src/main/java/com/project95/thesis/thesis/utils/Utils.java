package com.project95.thesis.thesis.utils;

public class Utils {

  private Utils() {
    // Private constructor to prevent instantiation
  }

  public static String normalize(String input) {
    if (input == null || input.isBlank()) {
      return null;
    }
    return input.trim();
  }

  public static <T> T unwrap(T value) {
    return value;
  }

  public static String sha256(String input) {
    if (input == null) {
      return null;
    }
    try {
      java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(hash);
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }
}
