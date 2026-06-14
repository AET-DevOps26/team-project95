package com.project95.thesis.thesis.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project95.thesis.thesis.domain.Chair;
import com.project95.thesis.thesis.domain.SourceEndpoint;
import com.project95.thesis.thesis.repository.ChairRepository;
import com.project95.thesis.thesis.repository.SourceEndpointRepository;
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
class InternalThesisControllerIntegrationTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ChairRepository chairRepository;
  @Autowired private SourceEndpointRepository sourceEndpointRepository;

  @BeforeEach
  void setUp() {
    sourceEndpointRepository.deleteAll();
    chairRepository.deleteAll();
  }

  @Test
  void listSourceEndpoints_ReturnsOnlyActiveEndpointsForScraping() throws Exception {
    Chair chair = chairRepository.save(new Chair("AI Chair", "https://ai.example.com/"));
    sourceEndpointRepository.save(endpoint(chair, "https://ai.example.com/active/", "ACTIVE"));
    sourceEndpointRepository.save(endpoint(chair, "https://ai.example.com/retired/", "RETIRED"));

    mockMvc
        .perform(get("/internal/v1/thesis-service/source-endpoints"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.endpoints", hasSize(1)))
        .andExpect(jsonPath("$.endpoints[0].url").value("https://ai.example.com/active/"))
        .andExpect(jsonPath("$.endpoints[0].status").value("ACTIVE"));
  }

  private static SourceEndpoint endpoint(Chair chair, String url, String status) {
    SourceEndpoint endpoint = new SourceEndpoint();
    endpoint.setChair(chair);
    endpoint.setUrl(url);
    endpoint.setStatus(status);
    return endpoint;
  }
}
