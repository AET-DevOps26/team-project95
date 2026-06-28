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
      byte[] encodedhash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      
      StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
      for (byte b : encodedhash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }
}
