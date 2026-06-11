package com.project95.thesis.thesis.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chairs")
public class Chair {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(name = "website_url", nullable = false, length = 1024)
  private String websiteUrl;

  @Column(name = "registry_key", unique = true)
  private String registryKey;

  // One Chair can have many Thesis Proposals.
  // If a Chair is deleted, its proposals are automatically removed (CascadeType.ALL +
  // orphanRemoval).
  @OneToMany(
      mappedBy = "chair",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<ThesisProposal> thesisProposals = new ArrayList<>();

  // One Chair can have multiple Source Endpoints to scrape from.
  @OneToMany(
      mappedBy = "chair",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<SourceEndpoint> sourceEndpoints = new ArrayList<>();

  public Chair() {}

  public Chair(String name, String websiteUrl) {
    this.name = name;
    this.websiteUrl = websiteUrl;
  }

  // --- Getters and Setters ---
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getWebsiteUrl() {
    return websiteUrl;
  }

  public void setWebsiteUrl(String websiteUrl) {
    this.websiteUrl = websiteUrl;
  }

  public String getRegistryKey() {
    return registryKey;
  }

  public void setRegistryKey(String registryKey) {
    this.registryKey = registryKey;
  }

  public List<ThesisProposal> getThesisProposals() {
    return thesisProposals;
  }

  public void setThesisProposals(List<ThesisProposal> thesisProposals) {
    this.thesisProposals = thesisProposals;
  }

  public List<SourceEndpoint> getSourceEndpoints() {
    return sourceEndpoints;
  }

  public void setSourceEndpoints(List<SourceEndpoint> sourceEndpoints) {
    this.sourceEndpoints = sourceEndpoints;
  }
}
