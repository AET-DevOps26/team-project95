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
}
