package com.project95.thesis.thesis.sourceconfig;

import com.project95.thesis.thesis.domain.Chair;
import com.project95.thesis.thesis.domain.SourceEndpoint;
import com.project95.thesis.thesis.repository.ChairRepository;
import com.project95.thesis.thesis.repository.SourceEndpointRepository;
import com.project95.thesis.thesis.sourceconfig.SourceRegistry.ChairEntry;
import com.project95.thesis.thesis.sourceconfig.SourceRegistry.EndpointEntry;
import com.project95.thesis.thesis.sourceconfig.SourceRegistry.RegistryResult;
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
public class SourceRegistryService {
  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String STATUS_RETIRED = "RETIRED";

  private static final Logger log = LoggerFactory.getLogger(SourceRegistryService.class);

  private final ChairRepository chairRepository;
  private final SourceEndpointRepository sourceEndpointRepository;
  private final SourceRegistryValidator validator;

  public SourceRegistryService(
      ChairRepository chairRepository,
      SourceEndpointRepository sourceEndpointRepository,
      SourceRegistryValidator validator) {
    this.chairRepository = chairRepository;
    this.sourceEndpointRepository = sourceEndpointRepository;
    this.validator = validator;
  }

  @Transactional
  public RegistryResult applyRegistry(List<ChairEntry> registry) {
    validator.validate(registry);

    RegistryStats stats = new RegistryStats();
    Map<String, Chair> chairsByRegistryKey = chairsByRegistryKey();
    Map<String, SourceEndpoint> endpointsByRegistryKey = endpointsByRegistryKey();
    Set<String> desiredEndpointKeys = new HashSet<>();

    for (ChairEntry registryChair : registry) {
      Chair chair = applyChair(registryChair, chairsByRegistryKey, stats);
      chairsByRegistryKey.put(chair.getRegistryKey(), chair);

      for (EndpointEntry registryEndpoint : registryChair.sourceEndpoints()) {
        desiredEndpointKeys.add(registryEndpoint.key().trim());
        SourceEndpoint endpoint =
            applyEndpoint(registryEndpoint, chair, endpointsByRegistryKey, stats);
        endpointsByRegistryKey.put(endpoint.getRegistryKey(), endpoint);
      }
    }

    retireRemovedEndpoints(desiredEndpointKeys, stats);
    RegistryResult result = stats.toResult();

    log.info(
        "Source endpoint registry applied. chairsInserted={}, chairsUpdated={},"
            + " endpointsInserted={}, endpointsUpdated={}, endpointsRetired={}",
        result.chairsInserted(),
        result.chairsUpdated(),
        result.endpointsInserted(),
        result.endpointsUpdated(),
        result.endpointsRetired());
    return result;
  }

  private Chair applyChair(
      ChairEntry registryChair, Map<String, Chair> chairsByRegistryKey, RegistryStats stats) {
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

    boolean changed =
        !Objects.equals(chair.getRegistryKey(), registryKey)
            || !Objects.equals(chair.getName(), name)
            || !Objects.equals(chair.getWebsiteUrl(), websiteUrl);

    chair.setRegistryKey(registryKey);
    chair.setName(name);
    chair.setWebsiteUrl(websiteUrl);

    if (changed) {
      stats.chairsUpdated++;
      chair = chairRepository.save(chair);
    }
    return chair;
  }

  private SourceEndpoint applyEndpoint(
      EndpointEntry registryEndpoint,
      Chair chair,
      Map<String, SourceEndpoint> endpointsByRegistryKey,
      RegistryStats stats) {
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

    boolean changed =
        !Objects.equals(endpoint.getRegistryKey(), registryKey)
            || !Objects.equals(endpoint.getUrl(), url)
            || !Objects.equals(endpoint.getStatus(), status)
            || endpoint.getChair() == null
            || !Objects.equals(endpoint.getChair().getId(), chair.getId());

    endpoint.setRegistryKey(registryKey);
    endpoint.setUrl(url);
    endpoint.setStatus(status);
    endpoint.setChair(chair);

    if (changed) {
      stats.endpointsUpdated++;
      endpoint = sourceEndpointRepository.save(endpoint);
    }
    return endpoint;
  }

  private void retireRemovedEndpoints(Set<String> desiredEndpointKeys, RegistryStats stats) {
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

  private static class RegistryStats {
    private int chairsInserted;
    private int chairsUpdated;
    private int endpointsInserted;
    private int endpointsUpdated;
    private int endpointsRetired;

    private RegistryResult toResult() {
      return new RegistryResult(
          chairsInserted, chairsUpdated, endpointsInserted, endpointsUpdated, endpointsRetired);
    }
  }
}
