package com.project95.thesis.thesis.sourceconfig;

import com.project95.thesis.thesis.sourceconfig.SourceRegistry.ChairEntry;
import com.project95.thesis.thesis.sourceconfig.SourceRegistry.EndpointEntry;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SourceRegistryValidator {
  private static final Set<String> SUPPORTED_STATUSES =
      Set.of(
          SourceEndpointRegistrySyncService.STATUS_ACTIVE,
          SourceEndpointRegistrySyncService.STATUS_RETIRED);

  public void validate(List<ChairEntry> registry) {
    if (registry == null) {
      throw new IllegalArgumentException("Source registry must not be null");
    }

    Set<String> chairKeys = new HashSet<>();
    Set<String> endpointKeys = new HashSet<>();
    Set<String> endpointUrls = new HashSet<>();

    for (ChairEntry chair : registry) {
      if (chair == null) {
        throw new IllegalArgumentException("Source registry must not contain null chairs");
      }
      String chairKey = requireText(chair.key(), "chair key");
      requireText(chair.name(), "chair name for key " + chairKey);
      validateUri(chair.websiteUrl(), "chair websiteUrl for key " + chairKey);

      if (!chairKeys.add(chairKey)) {
        throw new IllegalArgumentException("Duplicate chair key in source registry: " + chairKey);
      }
      if (chair.sourceEndpoints() == null) {
        throw new IllegalArgumentException("sourceEndpoints must not be null for chair " + chairKey);
      }

      for (EndpointEntry endpoint : chair.sourceEndpoints()) {
        if (endpoint == null) {
          throw new IllegalArgumentException("Chair " + chairKey + " contains a null endpoint");
        }
        String endpointKey = requireText(endpoint.key(), "endpoint key for chair " + chairKey);
        String endpointUrl = validateUri(endpoint.url(), "endpoint url for key " + endpointKey);
        String status = normalizeStatus(endpoint.status());
        if (!SUPPORTED_STATUSES.contains(status)) {
          throw new IllegalArgumentException(
              "Unsupported endpoint status " + endpoint.status() + " for key " + endpointKey);
        }
        if (!endpointKeys.add(endpointKey)) {
          throw new IllegalArgumentException("Duplicate endpoint key in source registry: " + endpointKey);
        }
        if (!endpointUrls.add(endpointUrl)) {
          throw new IllegalArgumentException("Duplicate endpoint URL in source registry: " + endpointUrl);
        }
      }
    }
  }

  static String normalizeStatus(String status) {
    return requireText(status, "endpoint status").toUpperCase(Locale.ROOT);
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException("Missing " + fieldName + " in source registry");
    }
    return value.trim();
  }

  private static String validateUri(String value, String fieldName) {
    String trimmed = requireText(value, fieldName);
    try {
      URI uri = new URI(trimmed);
      if (uri.getScheme() == null || uri.getHost() == null) {
        throw new IllegalArgumentException("Invalid " + fieldName + ": " + trimmed);
      }
      return trimmed;
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid " + fieldName + ": " + trimmed, e);
    }
  }
}
