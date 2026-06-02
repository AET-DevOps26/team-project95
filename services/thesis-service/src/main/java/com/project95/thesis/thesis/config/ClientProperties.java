package com.project95.thesis.thesis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.client")
public class ClientProperties {

  private VectorSearch vectorSearch = new VectorSearch();
  private Timeouts timeouts = new Timeouts();

  public VectorSearch getVectorSearch() {
    return vectorSearch;
  }

  public void setVectorSearch(VectorSearch vectorSearch) {
    this.vectorSearch = vectorSearch;
  }

  public Timeouts getTimeouts() {
    return timeouts;
  }

  public void setTimeouts(Timeouts timeouts) {
    this.timeouts = timeouts;
  }

  public static class VectorSearch {
    private String url;

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }
  }

  public static class Timeouts {
    private int connectMs;
    private int readMs;

    public int getConnectMs() {
      return connectMs;
    }

    public void setConnectMs(int connectMs) {
      this.connectMs = connectMs;
    }

    public int getReadMs() {
      return readMs;
    }

    public void setReadMs(int readMs) {
      this.readMs = readMs;
    }
  }
}
