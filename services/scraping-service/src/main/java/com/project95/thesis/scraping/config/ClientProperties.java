package com.project95.thesis.scraping.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.client")
public class ClientProperties {

  private MainThesis mainThesis = new MainThesis();
  private GenAi genAi = new GenAi();
  private Timeouts timeouts = new Timeouts();

  public MainThesis getMainThesis() {
    return mainThesis;
  }

  public void setMainThesis(MainThesis mainThesis) {
    this.mainThesis = mainThesis;
  }

  public GenAi getGenAi() {
    return genAi;
  }

  public void setGenAi(GenAi genAi) {
    this.genAi = genAi;
  }

  public Timeouts getTimeouts() {
    return timeouts;
  }

  public void setTimeouts(Timeouts timeouts) {
    this.timeouts = timeouts;
  }

  public static class MainThesis {
    private String url;

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }
  }

  public static class GenAi {
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
