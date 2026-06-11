package com.project95.thesis.thesis.sourceconfig;

import java.util.List;

public final class SourceRegistry {
  private SourceRegistry() {}

  public record ChairEntry(
      String key,
      String name,
      String websiteUrl,
      List<EndpointEntry> sourceEndpoints) {}

  public record EndpointEntry(String key, String url, String status) {}

  public record SyncResult(
      int chairsInserted,
      int chairsUpdated,
      int endpointsInserted,
      int endpointsUpdated,
      int endpointsRetired) {}
}
