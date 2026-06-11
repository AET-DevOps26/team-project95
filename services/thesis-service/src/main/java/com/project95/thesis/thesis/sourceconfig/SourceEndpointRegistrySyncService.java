package com.project95.thesis.thesis.sourceconfig;

import com.project95.thesis.thesis.domain.Chair;
import com.project95.thesis.thesis.domain.SourceEndpoint;
import com.project95.thesis.thesis.repository.ChairRepository;
import com.project95.thesis.thesis.repository.SourceEndpointRepository;
import com.project95.thesis.thesis.sourceconfig.SourceRegistry.ChairEntry;
import com.project95.thesis.thesis.sourceconfig.SourceRegistry.EndpointEntry;
import com.project95.thesis.thesis.sourceconfig.SourceRegistry.SyncResult;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourceEndpointRegistrySyncService {
  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String STATUS_RETIRED = "RETIRED";

  private static final Logger log = LoggerFactory.getLogger(SourceEndpointRegistrySyncService.class);

  private final ChairRepository chairRepository;
  private final SourceEndpointRepository sourceEndpointRepository;
  private final SourceRegistryValidator validator;

  public SourceEndpointRegistrySyncService(
      ChairRepository chairRepository,
      SourceEndpointRepository sourceEndpointRepository,
      SourceRegistryValidator validator) {
    this.chairRepository = chairRepository;
    this.sourceEndpointRepository = sourceEndpointRepository;
    this.validator = validator;
  }

  @Transactional
  public SyncResult syncFromRegistry(List<ChairEntry> registry) {
    validator.validate(registry);

    SyncStats stats = new SyncStats();
    Map<String, Chair> chairsByRegistryKey = chairsByRegistryKey();
    Map<String, SourceEndpoint> endpointsByRegistryKey = endpointsByRegistryKey();
    Set<String> desiredEndpointKeys = new HashSet<>();

    for (ChairEntry registryChair : registry) {
      Chair chair = syncChair(registryChair, chairsByRegistryKey, stats);
      chairsByRegistryKey.put(chair.getRegistryKey(), chair);

      for (EndpointEntry registryEndpoint : registryChair.sourceEndpoints()) {
        desiredEndpointKeys.add(registryEndpoint.key().trim());
        SourceEndpoint endpoint = syncEndpoint(registryEndpoint, chair, endpointsByRegistryKey, stats);
        endpointsByRegistryKey.put(endpoint.getRegistryKey(), endpoint);
      }
    }

    retireRemovedEndpoints(desiredEndpointKeys, stats);
    SyncResult result = stats.toResult();

    log.info(
        "Source endpoint registry sync completed. chairsInserted={}, chairsUpdated={}, endpointsInserted={}, endpointsUpdated={}, endpointsRetired={}",
        result.chairsInserted(),
        result.chairsUpdated(),
        result.endpointsInserted(),
        result.endpointsUpdated(),
        result.endpointsRetired());
    return result;
  }

  private Chair syncChair(
      ChairEntry registryChair, Map<String, Chair> chairsByRegistryKey, SyncStats stats) {
    String registryKey = registryChair.key().trim();
    String name = registryChair.name().trim();
    String websiteUrl = registryChair.websiteUrl().trim();

    Chair chair = chairsByRegistryKey.get(registryKey);
    if (chair == null) {
      chair = findExistingUnmanagedChair(name, websiteUrl).orElse(null);
    }

    if (chair == null) {
      chair = new Chair(name, websiteUrl);
      chair.setRegistryKey(registryKey);
      stats.chairsInserted++;
      return chairRepository.save(chair);
    }

    boolean changed = false;
    if (!Objects.equals(chair.getRegistryKey(), registryKey)) {
      chair.setRegistryKey(registryKey);
      changed = true;
    }
    if (!Objects.equals(chair.getName(), name)) {
      chair.setName(name);
      changed = true;
    }
    if (!Objects.equals(chair.getWebsiteUrl(), websiteUrl)) {
      chair.setWebsiteUrl(websiteUrl);
      changed = true;
    }

    if (changed) {
      stats.chairsUpdated++;
      chair = chairRepository.save(chair);
    }
    return chair;
  }

  private SourceEndpoint syncEndpoint(
      EndpointEntry registryEndpoint,
      Chair chair,
      Map<String, SourceEndpoint> endpointsByRegistryKey,
      SyncStats stats) {
    String registryKey = registryEndpoint.key().trim();
    String url = registryEndpoint.url().trim();
    String status = SourceRegistryValidator.normalizeStatus(registryEndpoint.status());

    SourceEndpoint endpoint = endpointsByRegistryKey.get(registryKey);
    if (endpoint == null) {
      endpoint = findExistingUnmanagedEndpoint(url).orElse(null);
    }

    if (endpoint == null) {
      endpoint = new SourceEndpoint();
      endpoint.setRegistryKey(registryKey);
      endpoint.setUrl(url);
      endpoint.setStatus(status);
      endpoint.setChair(chair);
      stats.endpointsInserted++;
      return sourceEndpointRepository.save(endpoint);
    }

    boolean changed = false;
    if (!Objects.equals(endpoint.getRegistryKey(), registryKey)) {
      endpoint.setRegistryKey(registryKey);
      changed = true;
    }
    if (!Objects.equals(endpoint.getUrl(), url)) {
      endpoint.setUrl(url);
      changed = true;
    }
    if (!Objects.equals(endpoint.getStatus(), status)) {
      endpoint.setStatus(status);
      changed = true;
    }
    if (endpoint.getChair() == null || !Objects.equals(endpoint.getChair().getId(), chair.getId())) {
      endpoint.setChair(chair);
      changed = true;
    }

    if (changed) {
      stats.endpointsUpdated++;
      endpoint = sourceEndpointRepository.save(endpoint);
    }
    return endpoint;
  }

  private void retireRemovedEndpoints(Set<String> desiredEndpointKeys, SyncStats stats) {
    for (SourceEndpoint endpoint : sourceEndpointRepository.findByRegistryKeyIsNotNull()) {
      if (!desiredEndpointKeys.contains(endpoint.getRegistryKey())
          && !STATUS_RETIRED.equals(endpoint.getStatus())) {
        endpoint.setStatus(STATUS_RETIRED);
        sourceEndpointRepository.save(endpoint);
        stats.endpointsRetired++;
      }
    }
  }

  private Map<String, Chair> chairsByRegistryKey() {
    Map<String, Chair> chairs = new HashMap<>();
    for (Chair chair : chairRepository.findAll()) {
      if (chair.getRegistryKey() != null) {
        chairs.put(chair.getRegistryKey(), chair);
      }
    }
    return chairs;
  }

  private Map<String, SourceEndpoint> endpointsByRegistryKey() {
    Map<String, SourceEndpoint> endpoints = new HashMap<>();
    for (SourceEndpoint endpoint : sourceEndpointRepository.findByRegistryKeyIsNotNull()) {
      endpoints.put(endpoint.getRegistryKey(), endpoint);
    }
    return endpoints;
  }

  private Optional<Chair> findExistingUnmanagedChair(String name, String websiteUrl) {
    return chairRepository.findAll().stream()
        .filter(chair -> chair.getRegistryKey() == null)
        .filter(chair -> Objects.equals(chair.getName(), name))
        .filter(chair -> Objects.equals(chair.getWebsiteUrl(), websiteUrl))
        .findFirst();
  }

  private Optional<SourceEndpoint> findExistingUnmanagedEndpoint(String url) {
    return sourceEndpointRepository.findAll().stream()
        .filter(endpoint -> endpoint.getRegistryKey() == null)
        .filter(endpoint -> Objects.equals(endpoint.getUrl(), url))
        .findFirst();
  }

  private static class SyncStats {
    private int chairsInserted;
    private int chairsUpdated;
    private int endpointsInserted;
    private int endpointsUpdated;
    private int endpointsRetired;

    private SyncResult toResult() {
      return new SyncResult(
          chairsInserted, chairsUpdated, endpointsInserted, endpointsUpdated, endpointsRetired);
    }
  }
}
