package com.project95.thesis.thesis.domain;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "advisors")
public class Advisor {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "profile_url", length = 1024)
  private String profileUrl;

  // Bi-directional many-to-many relationship mapping back to Thesis Proposals.
  @ManyToMany(mappedBy = "advisors", fetch = FetchType.LAZY)
  private Set<ThesisProposal> thesisProposals = new HashSet<>();

  public Advisor() {}

  public Advisor(String name, String email, String profileUrl) {
    this.name = name;
    this.email = email;
    this.profileUrl = profileUrl;
  }

  public void addThesisProposal(ThesisProposal proposal) {
    this.thesisProposals.add(proposal);
    if (!proposal.getAdvisors().contains(this)) {
      proposal.getAdvisors().add(this);
    }
  }

  public void removeThesisProposal(ThesisProposal proposal) {
    this.thesisProposals.remove(proposal);
    if (proposal.getAdvisors().contains(this)) {
      proposal.getAdvisors().remove(this);
    }
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

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getProfileUrl() {
    return profileUrl;
  }

  public void setProfileUrl(String profileUrl) {
    this.profileUrl = profileUrl;
  }

  public Set<ThesisProposal> getThesisProposals() {
    return thesisProposals;
  }

  public void setThesisProposals(Set<ThesisProposal> thesisProposals) {
    this.thesisProposals = thesisProposals;
  }
}
