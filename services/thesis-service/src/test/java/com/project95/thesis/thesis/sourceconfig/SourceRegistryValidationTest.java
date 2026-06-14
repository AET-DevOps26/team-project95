package com.project95.thesis.thesis.sourceconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project95.thesis.thesis.sourceconfig.SourceRegistry.ChairEntry;
import com.project95.thesis.thesis.sourceconfig.SourceRegistry.EndpointEntry;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class SourceRegistryValidationTest {
  private final SourceRegistryValidator validator = new SourceRegistryValidator();

  @Test
  void validate_AcceptsValidRegistry() {
    assertThatCode(() -> validator.validate(List.of(chair("chair-a", endpoint("endpoint-a")))))
        .doesNotThrowAnyException();
  }

  @Test
  void validate_AcceptsCommittedSourceEndpointRegistry() throws Exception {
    try (InputStream inputStream = getClass().getResourceAsStream("/source-endpoints.json")) {
      assertThat(inputStream).isNotNull();
      List<ChairEntry> registry = new ObjectMapper().readValue(inputStream, new TypeReference<>() {});

      assertThatCode(() -> validator.validate(registry)).doesNotThrowAnyException();
      assertThat(registry).isNotEmpty();
    }
  }

  @Test
  void validate_RejectsDuplicateChairKeys() {
    List<ChairEntry> registry =
        List.of(chair("chair-a", endpoint("endpoint-a")), chair("chair-a", endpoint("endpoint-b")));

    assertThatThrownBy(() -> validator.validate(registry))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Duplicate chair key");
  }

  @Test
  void validate_RejectsDuplicateEndpointKeys() {
    List<ChairEntry> registry =
        List.of(chair("chair-a", endpoint("endpoint-a")), chair("chair-b", endpoint("endpoint-a")));

    assertThatThrownBy(() -> validator.validate(registry))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Duplicate endpoint key");
  }

  @Test
  void validate_RejectsUnsupportedStatus() {
    EndpointEntry endpoint = endpoint("endpoint-a", "BROKEN");

    assertThatThrownBy(() -> validator.validate(List.of(chair("chair-a", endpoint))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported endpoint status");
  }

  @Test
  void validate_RejectsInvalidUrl() {
    EndpointEntry endpoint = new EndpointEntry("endpoint-a", "not-a-url", "ACTIVE");

    assertThatThrownBy(() -> validator.validate(List.of(chair("chair-a", endpoint))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid endpoint url");
  }

  private static ChairEntry chair(String key, EndpointEntry endpoint) {
    return new ChairEntry(
        key, "Chair " + key, "https://example.com/" + key + "/", List.of(endpoint));
  }

  private static EndpointEntry endpoint(String key) {
    return endpoint(key, "ACTIVE");
  }

  private static EndpointEntry endpoint(String key, String status) {
    return new EndpointEntry(key, "https://example.com/" + key + "/theses/", status);
  }
}
