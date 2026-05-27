package com.project95.thesis.thesis.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "source_endpoints")
public class SourceEndpoint {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 1024)
  private String url;

  @Column(nullable = false)
  private String status; // e.g., "ACTIVE"

  @Column(name = "last_scraped_at")
  private OffsetDateTime lastScrapedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chair_id", nullable = false)
  private Chair chair;

  // One SourceEndpoint logs historical tracking data via Scrape Runs.
  @OneToMany(
      mappedBy = "sourceEndpoint",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<ScrapeRun> scrapeRuns = new ArrayList<>();

  public SourceEndpoint() {}

  // --- Getters and Setters ---
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public OffsetDateTime getLastScrapedAt() {
    return lastScrapedAt;
  }

  public void setLastScrapedAt(OffsetDateTime lastScrapedAt) {
    this.lastScrapedAt = lastScrapedAt;
  }

  public Chair getChair() {
    return chair;
  }

  public void setChair(Chair chair) {
    this.chair = chair;
  }

  public List<ScrapeRun> getScrapeRuns() {
    return scrapeRuns;
  }

  public void setScrapeRuns(List<ScrapeRun> scrapeRuns) {
    this.scrapeRuns = scrapeRuns;
  }
}
