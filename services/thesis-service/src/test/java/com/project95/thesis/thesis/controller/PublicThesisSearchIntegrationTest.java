package com.project95.thesis.thesis.controller;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project95.thesis.management.dto.SearchThesesRequestDto;
import com.project95.thesis.management.dto.ThesisSearchFiltersDto;
import com.project95.thesis.thesis.domain.Chair;
import com.project95.thesis.thesis.domain.ResearchArea;
import com.project95.thesis.thesis.domain.SourceEndpoint;
import com.project95.thesis.thesis.domain.Tag;
import com.project95.thesis.thesis.domain.ThesisProposal;
import com.project95.thesis.thesis.repository.*;
import java.util.List;
import java.util.Set;
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
class PublicThesisSearchIntegrationTest {

  private static final String OPENAPI_SPEC = "../../api/openapi-v1.yml";

  @Autowired private MockMvc mockMvc;
  private final ObjectMapper objectMapper =
      new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

  @Autowired private ThesisProposalRepository thesisRepository;
  @Autowired private ChairRepository chairRepository;
  @Autowired private SourceEndpointRepository sourceEndpointRepository;
  @Autowired private TagRepository tagRepository;
  @Autowired private ResearchAreaRepository researchAreaRepository;

  private static MockRestServiceServer mockServer;
  private ThesisProposal savedThesis1;
  private ThesisProposal savedThesis2;
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
    sourceEndpointRepository.deleteAll();
    chairRepository.deleteAll();
    tagRepository.deleteAll();
    researchAreaRepository.deleteAll();

    chair = chairRepository.save(new Chair("AI Chair", "http://ai.tum.de"));
    SourceEndpoint sourceEndpoint = new SourceEndpoint();
    sourceEndpoint.setChair(chair);
    sourceEndpoint.setUrl("http://ai.tum.de/theses");
    sourceEndpoint.setStatus("ACTIVE");
    sourceEndpoint = sourceEndpointRepository.save(sourceEndpoint);

    Tag tag = tagRepository.save(new Tag("LLM"));
    ResearchArea area = researchAreaRepository.save(new ResearchArea("NLP"));

    ThesisProposal t1 = new ThesisProposal();
    t1.setTitle("First Thesis Title");
    t1.setChair(chair);
    t1.setSourceEndpoint(sourceEndpoint);
    t1.setSourceUrl("http://test.com/t1");
    t1.setStatus("OPEN");
    t1.setDegreeType("MASTER");
    t1.setTags(Set.of(tag));
    t1.setResearchAreas(Set.of(area));
    t1.setOriginalDescription("This is the first thesis proposal description.");
    t1.setAiOverview("First thesis summary.");
    savedThesis1 = thesisRepository.save(t1);

    ThesisProposal t2 = new ThesisProposal();
    t2.setTitle("Second Thesis Title");
    t2.setChair(chair);
    t2.setSourceEndpoint(sourceEndpoint);
    t2.setSourceUrl("http://test.com/t2");
    t2.setStatus("OPEN");
    t2.setDegreeType("BACHELOR");
    t2.setOriginalDescription("This is the second thesis proposal description.");
    t2.setAiOverview("Second thesis summary.");
    savedThesis2 = thesisRepository.save(t2);
  }

  @Test
  void listTheses_ReturnsAll() throws Exception {
    mockMvc
        .perform(get("/api/v1/theses"))
        .andExpect(status().isOk())
        .andExpect(openApi().isValid(OPENAPI_SPEC))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(
            jsonPath(
                "$[*].title", containsInAnyOrder("First Thesis Title", "Second Thesis Title")));
  }

  @Test
  void getThesisById_ReturnsThesis() throws Exception {
    mockMvc
        .perform(get("/api/v1/theses/" + savedThesis1.getId()))
        .andExpect(status().isOk())
        .andExpect(openApi().isValid(OPENAPI_SPEC))
        .andExpect(jsonPath("$.title").value("First Thesis Title"))
        .andExpect(jsonPath("$.degreeType").value("MASTER"))
        .andExpect(jsonPath("$.tags", contains("LLM")))
        .andExpect(jsonPath("$.researchArea").value("NLP"));
  }

  @Test
  void getThesisById_NotFound() throws Exception {
    mockMvc.perform(get("/api/v1/theses/99999")).andExpect(status().isNotFound());
  }

  @Test
  void searchTheses_RelationalFiltersOnly() throws Exception {
    SearchThesesRequestDto request = new SearchThesesRequestDto();
    ThesisSearchFiltersDto filters = new ThesisSearchFiltersDto();
    filters.setDegreeTypes(List.of("MASTER"));
    request.setFilters(filters);

    mockMvc
        .perform(
            post("/api/v1/theses/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(openApi().isValid(OPENAPI_SPEC))
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].id").value(savedThesis1.getId()))
        .andExpect(jsonPath("$.items[0].title").value("First Thesis Title"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void searchTheses_FilterByChair() throws Exception {
    SearchThesesRequestDto request = new SearchThesesRequestDto();
    ThesisSearchFiltersDto filters = new ThesisSearchFiltersDto();
    filters.setChairIds(List.of(chair.getId()));
    request.setFilters(filters);

    mockMvc
        .perform(
            post("/api/v1/theses/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  void searchTheses_FilterByTag() throws Exception {
    SearchThesesRequestDto request = new SearchThesesRequestDto();
    ThesisSearchFiltersDto filters = new ThesisSearchFiltersDto();
    filters.setTags(List.of("LLM"));
    request.setFilters(filters);

    mockMvc
        .perform(
            post("/api/v1/theses/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].id").value(savedThesis1.getId()))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void searchTheses_FilterByResearchArea() throws Exception {
    SearchThesesRequestDto request = new SearchThesesRequestDto();
    ThesisSearchFiltersDto filters = new ThesisSearchFiltersDto();
    filters.setResearchAreas(List.of("NLP"));
    request.setFilters(filters);

    mockMvc
        .perform(
            post("/api/v1/theses/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].id").value(savedThesis1.getId()))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void searchTheses_FilterByStatus() throws Exception {
    SearchThesesRequestDto request = new SearchThesesRequestDto();
    ThesisSearchFiltersDto filters = new ThesisSearchFiltersDto();
    filters.setStatus("OPEN");
    request.setFilters(filters);

    mockMvc
        .perform(
            post("/api/v1/theses/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  void searchTheses_HybridSearchSuccess() throws Exception {
    SearchThesesRequestDto request = new SearchThesesRequestDto();
    request.setNaturalLanguageQuery("semantic search LLM");
    ThesisSearchFiltersDto filters = new ThesisSearchFiltersDto();
    filters.setChairIds(List.of(chair.getId()));
    request.setFilters(filters);

    String vectorSearchResponseJson =
        "{\"results\":["
            + "{\"thesisId\":"
            + savedThesis2.getId()
            + ",\"score\":0.9},"
            + "{\"thesisId\":"
            + savedThesis1.getId()
            + ",\"score\":0.6}"
            + "]}";

    mockServer
        .expect(requestTo("http://localhost:8082/internal/v1/vector-search-service/search"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(vectorSearchResponseJson, MediaType.APPLICATION_JSON));

    // Assert: Order matches semantic score (Thesis 2 then Thesis 1)
    mockMvc
        .perform(
            post("/api/v1/theses/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.items[0].id").value(savedThesis2.getId()))
        .andExpect(jsonPath("$.items[0].semanticScore").value(0.9f))
        .andExpect(jsonPath("$.items[1].id").value(savedThesis1.getId()))
        .andExpect(jsonPath("$.items[1].semanticScore").value(0.6f));

    mockServer.verify();
  }

  @Test
  void searchTheses_VectorSearchFailureFallback() throws Exception {
    SearchThesesRequestDto request = new SearchThesesRequestDto();
    request.setNaturalLanguageQuery("semantic search LLM");
    ThesisSearchFiltersDto filters = new ThesisSearchFiltersDto();
    filters.setDegreeTypes(List.of("BACHELOR"));
    request.setFilters(filters);

    // Mock Vector Service failing with 500
    mockServer
        .expect(requestTo("http://localhost:8082/internal/v1/vector-search-service/search"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withServerError());

    // Assert: Search gracefully falls back to relational database search
    mockMvc
        .perform(
            post("/api/v1/theses/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].id").value(savedThesis2.getId()))
        .andExpect(jsonPath("$.items[0].semanticScore").isEmpty());

    mockServer.verify();
  }
}
