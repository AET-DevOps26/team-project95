package com.project95.thesis.thesis.domain;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
public class Tag {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false, length = 100)
  private String name;

  // Bi-directional many-to-many link to access theses belonging to a specific tag.
  @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
  private Set<ThesisProposal> thesisProposals = new HashSet<>();

  public Tag() {}

  public Tag(String name) {
    this.name = name;
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

  public Set<ThesisProposal> getThesisProposals() {
    return thesisProposals;
  }

  public void setThesisProposals(Set<ThesisProposal> thesisProposals) {
    this.thesisProposals = thesisProposals;
  }
}
