package com.project95.thesis.thesis.sourceconfig;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project95.thesis.thesis.sourceconfig.SourceRegistry.ChairEntry;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class SourceRegistrySyncRunner implements ApplicationRunner {
  private static final String SYNC_SOURCE_REGISTRY_OPTION = "sync-source-registry";
  private static final TypeReference<List<ChairEntry>> REGISTRY_TYPE = new TypeReference<>() {};

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ResourceLoader resourceLoader;
  private final SourceEndpointRegistrySyncService syncService;
  private final String registryPath;

  public SourceRegistrySyncRunner(
      ResourceLoader resourceLoader,
      SourceEndpointRegistrySyncService syncService,
      @Value("${app.source-registry.path:classpath:source-endpoints.json}") String registryPath) {
    this.resourceLoader = resourceLoader;
    this.syncService = syncService;
    this.registryPath = registryPath;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!args.containsOption(SYNC_SOURCE_REGISTRY_OPTION)) {
      return;
    }

    syncService.syncFromRegistry(loadRegistry());
  }

  private List<ChairEntry> loadRegistry() {
    Resource resource = resourceLoader.getResource(registryPath);
    if (!resource.exists()) {
      throw new IllegalStateException("Source registry not found: " + registryPath);
    }

    try (InputStream inputStream = resource.getInputStream()) {
      return objectMapper.readValue(inputStream, REGISTRY_TYPE);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read source registry: " + registryPath, e);
    }
  }
}
