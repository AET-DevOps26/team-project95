package com.project95.thesis.thesis.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "thesis_proposals")
public class ThesisProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "degree_type")
    private String degreeType;

    @Column(name = "original_description", columnDefinition = "TEXT")
    private String originalDescription;

    @Column(name = "ai_overview", columnDefinition = "TEXT")
    private String aiOverview;

    @Column(name = "source_url", nullable = false, length = 1024)
    private String sourceUrl;

    @Column(name = "raw_html_snapshot", columnDefinition = "TEXT")
    private String rawHtmlSnapshot;

    @Column(name = "extraction_confidence")
    private Float extractionConfidence;

    @Column(nullable = false)
    private String status = "OPEN";

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chair_id", nullable = false)
    private Chair chair;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "thesis_proposal_advisors",
        joinColumns = @JoinColumn(name = "thesis_proposal_id"),
        inverseJoinColumns = @JoinColumn(name = "advisor_id")
    )
    private Set<Advisor> advisors = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "thesis_proposal_tags",
        joinColumns = @JoinColumn(name = "thesis_proposal_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "thesis_proposal_research_areas",
        joinColumns = @JoinColumn(name = "thesis_proposal_id"),
        inverseJoinColumns = @JoinColumn(name = "research_area_id")
    )
    private Set<ResearchArea> researchAreas = new HashSet<>();

    public ThesisProposal() {}

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDegreeType() { return degreeType; }
    public void setDegreeType(String degreeType) { this.degreeType = degreeType; }

    public String getOriginalDescription() { return originalDescription; }
    public void setOriginalDescription(String originalDescription) { this.originalDescription = originalDescription; }

    public String getAiOverview() { return aiOverview; }
    public void setAiOverview(String aiOverview) { this.aiOverview = aiOverview; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getRawHtmlSnapshot() { return rawHtmlSnapshot; }
    public void setRawHtmlSnapshot(String rawHtmlSnapshot) { this.rawHtmlSnapshot = rawHtmlSnapshot; }

    public Float getExtractionConfidence() { return extractionConfidence; }
    public void setExtractionConfidence(Float extractionConfidence) { this.extractionConfidence = extractionConfidence; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(OffsetDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public Chair getChair() { return chair; }
    public void setChair(Chair chair) { this.chair = chair; }

    public Set<Advisor> getAdvisors() { return advisors; }
    public void setAdvisors(Set<Advisor> advisors) { this.advisors = advisors; }

    public Set<Tag> getTags() { return tags; }
    public void setTags(Set<Tag> tags) { this.tags = tags; }

    public Set<ResearchArea> getResearchAreas() { return researchAreas; }
    public void setResearchAreas(Set<ResearchArea> researchAreas) { this.researchAreas = researchAreas; }
}
