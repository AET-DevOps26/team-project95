package com.project95.thesis.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project95.thesis.management.dto.ScrapeRunLogRequestDto;
import com.project95.thesis.management.dto.SourceEndpointThesesReplacementRequestDto;
import com.project95.thesis.management.dto.ThesisProposalInputDto;
import com.project95.thesis.thesis.domain.Chair;
import com.project95.thesis.thesis.domain.ScrapeRun;
import com.project95.thesis.thesis.domain.SourceEndpoint;
import com.project95.thesis.thesis.domain.ThesisProposal;
import com.project95.thesis.thesis.repository.ChairRepository;
import com.project95.thesis.thesis.repository.ScrapeRunRepository;
import com.project95.thesis.thesis.repository.SourceEndpointRepository;
import com.project95.thesis.thesis.repository.ThesisProposalRepository;
import com.project95.thesis.thesis.utils.HtmlNormalizer;
import com.project95.thesis.thesis.utils.Utils;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InternalThesisControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  private final ObjectMapper objectMapper =
      new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

  @Autowired private ChairRepository chairRepository;
  @Autowired private SourceEndpointRepository sourceEndpointRepository;
  @Autowired private ScrapeRunRepository scrapeRunRepository;
  @Autowired private ThesisProposalRepository thesisRepository;

  private static MockRestServiceServer mockServer;
  private SourceEndpoint activeEndpoint;
  private Chair chair;

  @TestConfiguration
  static class TestConfig {
    @Bean
    @Primary
    public RestClient restClient(RestClient.Builder builder) {
      mockServer = MockRestServiceServer.bindTo(builder).build();
      return builder.baseUrl("http://localhost:8082").build();
    }
  }

  @BeforeEach
  void setUp() {
    mockServer.reset();

    thesisRepository.deleteAll();
    scrapeRunRepository.deleteAll();
    sourceEndpointRepository.deleteAll();
    chairRepository.deleteAll();

    chair = chairRepository.save(new Chair("AI Chair", "https://ai.example.com/"));
    activeEndpoint =
        sourceEndpointRepository.save(endpoint(chair, "https://ai.example.com/active/", "ACTIVE"));
  }

  @Test
  void listSourceEndpoints_ReturnsOnlyActiveEndpointsForScraping() throws Exception {
    sourceEndpointRepository.save(endpoint(chair, "https://ai.example.com/retired/", "RETIRED"));

    mockMvc
        .perform(get("/internal/v1/thesis-service/source-endpoints"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.endpoints", hasSize(1)))
        .andExpect(jsonPath("$.endpoints[0].url").value("https://ai.example.com/active/"))
        .andExpect(jsonPath("$.endpoints[0].status").value("ACTIVE"));
  }

  @Test
  void logScrapeRun_Success_PersistsRunAndUpdatesEndpoint() throws Exception {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    ScrapeRunLogRequestDto request = new ScrapeRunLogRequestDto();
    request.setSourceEndpointId(activeEndpoint.getId());
    request.setStartedAt(now.minusMinutes(5));
    request.setFinishedAt(now);
    request.setStatus(ScrapeRunLogRequestDto.StatusEnum.SUCCESS);
    request.setCandidatesFound(3);
    request.setRawHtmlSnapshot("<html></html>");

    mockMvc
        .perform(
            post("/internal/v1/thesis-service/scrape-runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.status").value("SUCCESS"));

    // Verify DB state
    List<ScrapeRun> runs = scrapeRunRepository.findAll();
    assertThat(runs).hasSize(1);
    assertThat(runs.get(0).getStatus()).isEqualTo("SUCCESS");
    assertThat(runs.get(0).getRawHtmlSnapshot()).isEqualTo("<html></html>");
    assertThat(runs.get(0).getCandidatesFound()).isEqualTo(3);

    // Verify lastScrapedAt updated on endpoint
    SourceEndpoint updatedEndpoint =
        sourceEndpointRepository.findById(activeEndpoint.getId()).orElseThrow();
    assertThat(updatedEndpoint.getLastScrapedAt()).isNotNull();
  }

  @Test
  void logScrapeRun_ValidationError_ReturnsBadRequest() throws Exception {
    ScrapeRunLogRequestDto request = new ScrapeRunLogRequestDto();
    request.setSourceEndpointId(activeEndpoint.getId());
    // startedAt is missing, status is missing (validation should fail)

    mockMvc
        .perform(
            post("/internal/v1/thesis-service/scrape-runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void replaceChairTheses_Success_UpdatesDBAndSyncsVectors() throws Exception {
    ThesisProposalInputDto inputDto = new ThesisProposalInputDto();
    inputDto.setTitle("Vector Search Thesis");
    inputDto.setDegreeType("MASTER");
    inputDto.setSourceUrl(URI.create("https://ai.example.com/vector-search"));
    inputDto.setStatus("OPEN");

    SourceEndpointThesesReplacementRequestDto request =
        new SourceEndpointThesesReplacementRequestDto();
    request.setTheses(List.of(inputDto));

    // Mock vector-search-service response
    String vectorSyncResponseJson = "{\"insertedVectorEntries\":1,\"replacedVectorEntries\":1}";
    mockServer
        .expect(
            requestTo(
                "http://localhost:8082/internal/v1/vector-search-service/source-endpoints/"
                    + activeEndpoint.getId()
                    + "/index"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(vectorSyncResponseJson, MediaType.APPLICATION_JSON));

    mockMvc
        .perform(
            put("/internal/v1/thesis-service/source-endpoints/"
                    + activeEndpoint.getId()
                    + "/theses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sourceEndpointId").value(activeEndpoint.getId()))
        .andExpect(jsonPath("$.insertedRelationalTheses").value(1))
        .andExpect(jsonPath("$.replacedVectorEntries").value(1))
        .andExpect(jsonPath("$.errorMessage").isEmpty());

    // Verify DB state
    List<ThesisProposal> proposals = thesisRepository.findAll();
    assertThat(proposals).hasSize(1);
    assertThat(proposals.get(0).getTitle()).isEqualTo("Vector Search Thesis");

    mockServer.verify();
  }

  @Test
  void replaceChairTheses_VectorSyncFailure_CommitsRelationalDataAndReturnsWarning()
      throws Exception {
    ThesisProposalInputDto inputDto = new ThesisProposalInputDto();
    inputDto.setTitle("Vector Search Resilient Thesis");
    inputDto.setDegreeType("MASTER");
    inputDto.setSourceUrl(URI.create("https://ai.example.com/vector-search-resilient"));
    inputDto.setStatus("OPEN");

    SourceEndpointThesesReplacementRequestDto request =
        new SourceEndpointThesesReplacementRequestDto();
    request.setTheses(List.of(inputDto));

    // Mock vector-search-service throwing 500 error
    mockServer
        .expect(
            requestTo(
                "http://localhost:8082/internal/v1/vector-search-service/source-endpoints/"
                    + activeEndpoint.getId()
                    + "/index"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withServerError());

    mockMvc
        .perform(
            put("/internal/v1/thesis-service/source-endpoints/"
                    + activeEndpoint.getId()
                    + "/theses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sourceEndpointId").value(activeEndpoint.getId()))
        .andExpect(jsonPath("$.insertedRelationalTheses").value(1))
        .andExpect(jsonPath("$.replacedVectorEntries").value(0))
        .andExpect(jsonPath("$.errorMessage").value(containsString("Vector sync failed")));

    // Verify DB state: Relational database update is committed despite vector service failing
    List<ThesisProposal> proposals = thesisRepository.findAll();
    assertThat(proposals).hasSize(1);
    assertThat(proposals.get(0).getTitle()).isEqualTo("Vector Search Resilient Thesis");

    mockServer.verify();
  }

  @Test
  void detectChanges_NoPreviousHash_ReturnsChangedTrue() throws Exception {
    com.project95.thesis.management.dto.DetectChangesRequestDto request =
        new com.project95.thesis.management.dto.DetectChangesRequestDto();
    request.setRawHtml("<html><body><h1>AI Thesis</h1></body></html>");

    mockMvc
        .perform(
            post("/thesis-internal/v1/source-endpoints/"
                    + activeEndpoint.getId()
                    + "/detect-changes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.changed").value(true))
        .andExpect(jsonPath("$.contentHash").isNotEmpty())
        .andExpect(jsonPath("$.sanitizedHtml").value(containsString("AI Thesis")));
  }

  @Test
  void detectChanges_MatchingHash_ReturnsChangedFalse() throws Exception {
    // 1. Calculate and set the hash in the DB
    String sanitizedHtml =
        HtmlNormalizer.sanitizeHtml(
            "<html><body><h1>AI Thesis</h1></body></html>", activeEndpoint.getUrl());
    String normalizedText = HtmlNormalizer.getNormalizedText(sanitizedHtml);
    String hash = Utils.sha256(normalizedText);

    activeEndpoint.setLastContentHash(hash);
    sourceEndpointRepository.save(activeEndpoint);

    // 2. Perform request with same HTML content
    com.project95.thesis.management.dto.DetectChangesRequestDto request =
        new com.project95.thesis.management.dto.DetectChangesRequestDto();
    request.setRawHtml("<html><body><h1>AI Thesis</h1></body></html>");

    mockMvc
        .perform(
            post("/thesis-internal/v1/source-endpoints/"
                    + activeEndpoint.getId()
                    + "/detect-changes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.changed").value(false))
        .andExpect(jsonPath("$.contentHash").value(hash));
  }

  @Test
  void detectChanges_EndpointNotFound_ReturnsNotFound() throws Exception {
    com.project95.thesis.management.dto.DetectChangesRequestDto request =
        new com.project95.thesis.management.dto.DetectChangesRequestDto();
    request.setRawHtml("<html><body><h1>AI Thesis</h1></body></html>");

    mockMvc
        .perform(
            post("/thesis-internal/v1/source-endpoints/"
                    + (activeEndpoint.getId() + 9999L)
                    + "/detect-changes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.message").value(containsString("Source endpoint not found with ID:")));
  }

  private static SourceEndpoint endpoint(Chair chair, String url, String status) {
    SourceEndpoint endpoint = new SourceEndpoint();
    endpoint.setChair(chair);
    endpoint.setUrl(url);
    endpoint.setStatus(status);
    return endpoint;
  }
}
