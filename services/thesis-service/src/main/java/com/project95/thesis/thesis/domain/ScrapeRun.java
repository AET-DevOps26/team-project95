package com.project95.thesis.thesis.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "scrape_runs")
public class ScrapeRun {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "started_at", nullable = false)
  private OffsetDateTime startedAt;

  @Column(name = "finished_at")
  private OffsetDateTime finishedAt;

  @Column(nullable = false)
  private String status; // e.g., "SUCCESS", "FAILED"

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "raw_html_snapshot", columnDefinition = "TEXT")
  private String rawHtmlSnapshot;

  @Column(name = "candidates_found")
  private Integer candidatesFound = 0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_endpoint_id", nullable = false)
  private SourceEndpoint sourceEndpoint;

  public ScrapeRun() {}

  // --- Getters and Setters ---
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public OffsetDateTime getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(OffsetDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public OffsetDateTime getFinishedAt() {
    return finishedAt;
  }

  public void setFinishedAt(OffsetDateTime finishedAt) {
    this.finishedAt = finishedAt;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getRawHtmlSnapshot() {
    return rawHtmlSnapshot;
  }

  public void setRawHtmlSnapshot(String rawHtmlSnapshot) {
    this.rawHtmlSnapshot = rawHtmlSnapshot;
  }

  public Integer getCandidatesFound() {
    return candidatesFound;
  }

  public void setCandidatesFound(Integer candidatesFound) {
    this.candidatesFound = candidatesFound;
  }

  public SourceEndpoint getSourceEndpoint() {
    return sourceEndpoint;
  }

  public void setSourceEndpoint(SourceEndpoint sourceEndpoint) {
    this.sourceEndpoint = sourceEndpoint;
  }
}
