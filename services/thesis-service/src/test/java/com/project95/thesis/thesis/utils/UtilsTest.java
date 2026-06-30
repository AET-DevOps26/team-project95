package com.project95.thesis.thesis.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UtilsTest {

  @Test
  void sha256_ComputesCorrectHash() {
    String input = "Hello World";
    // Expected SHA-256 hash for "Hello World"
    String expectedHash = "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e";

    String hash = Utils.sha256(input);
    assertThat(hash).isEqualTo(expectedHash);
  }

  @Test
  void sha256_ReturnsNullOnNullInput() {
    assertThat(Utils.sha256(null)).isNull();
  }
}
