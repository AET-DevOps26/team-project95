package com.project95.thesis.thesis.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.project95.thesis.thesis.domain.Chair;
import com.project95.thesis.thesis.domain.ResearchArea;
import com.project95.thesis.thesis.domain.SourceEndpoint;
import com.project95.thesis.thesis.domain.Tag;
import com.project95.thesis.thesis.domain.ThesisProposal;
import com.project95.thesis.thesis.repository.ChairRepository;
import com.project95.thesis.thesis.repository.ResearchAreaRepository;
import com.project95.thesis.thesis.repository.SourceEndpointRepository;
import com.project95.thesis.thesis.repository.TagRepository;
import com.project95.thesis.thesis.repository.ThesisProposalRepository;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FilterControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ChairRepository chairRepository;
  @Autowired private SourceEndpointRepository sourceEndpointRepository;

  @Autowired private TagRepository tagRepository;

  @Autowired private ResearchAreaRepository researchAreaRepository;

  @Autowired private ThesisProposalRepository thesisRepository;

  @BeforeEach
  void setUp() {
    thesisRepository.deleteAll();
    sourceEndpointRepository.deleteAll();
    chairRepository.deleteAll();
    tagRepository.deleteAll();
    researchAreaRepository.deleteAll();

    Chair chair = chairRepository.save(new Chair("AI Chair", "http://ai.tum.de"));
    SourceEndpoint sourceEndpoint = new SourceEndpoint();
    sourceEndpoint.setChair(chair);
    sourceEndpoint.setUrl("http://ai.tum.de/theses");
    sourceEndpoint.setStatus("ACTIVE");
    sourceEndpoint = sourceEndpointRepository.save(sourceEndpoint);

    Tag tag = tagRepository.save(new Tag("LLM"));
    ResearchArea area = researchAreaRepository.save(new ResearchArea("NLP"));

    ThesisProposal t = new ThesisProposal();
    t.setTitle("Test Thesis");
    t.setChair(chair);
    t.setSourceEndpoint(sourceEndpoint);
    t.setSourceUrl("http://test.com");
    t.setStatus("OPEN");
    t.setTags(Set.of(tag));
    t.setResearchAreas(Set.of(area));
    thesisRepository.save(t);
  }

  @Test
  void listChairs_ReturnsAllChairs() throws Exception {
    mockMvc
        .perform(get("/api/v1/chairs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name").value("AI Chair"));
  }

  @Test
  void getAvailableFilters_AggregatesMetadata() throws Exception {
    mockMvc
        .perform(get("/api/v1/filters"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.chairs", hasSize(1)))
        .andExpect(jsonPath("$.tags", contains("LLM")))
        .andExpect(jsonPath("$.researchAreas", contains("NLP")))
        .andExpect(jsonPath("$.degreeTypes", hasItem("MASTER")));
  }
}

