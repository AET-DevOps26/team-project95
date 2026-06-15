package com.project95.thesis.thesis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.source-registry")
public class SourceRegistryProperties {
  private String path = "classpath:source-endpoints.json";

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }
}
