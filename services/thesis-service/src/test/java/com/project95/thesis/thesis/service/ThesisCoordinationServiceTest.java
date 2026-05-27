package com.project95.thesis.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.project95.thesis.management.dto.ChairThesesReplacementRequest;
import com.project95.thesis.management.dto.ChairThesesReplacementResponse;
import com.project95.thesis.management.dto.ScrapeRunLogResponse;
import com.project95.thesis.thesis.config.ClientProperties;
import com.project95.thesis.thesis.domain.ThesisProposal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class ThesisCoordinationServiceTest {

  @Mock private ThesisManagementService thesisManagementService;
  @Mock private ScrapeRunService scrapeRunService;

  private MockRestServiceServer mockServer;
  private ThesisCoordinationService service;

  @BeforeEach
  void setUp() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

    ClientProperties clientProperties = new ClientProperties();
    clientProperties.getVectorSearch().setUrl("http://vector-service");

    service =
        new ThesisCoordinationService(
            thesisManagementService, scrapeRunService, restClientBuilder.build(), clientProperties);
  }

  @Test
  void executeScrapeIngestionPipeline_CallsVectorService() {
    // Arrange
    Long chairId = 1L;
    ChairThesesReplacementRequest request = new ChairThesesReplacementRequest();
    request.setSourceEndpointId(10L);
    request.setStatus(ChairThesesReplacementRequest.StatusEnum.SUCCESS);

    ThesisProposal persistentThesis = new ThesisProposal();
    persistentThesis.setId(100L);
    persistentThesis.setTitle("Vector Sync Test");

    ScrapeRunLogResponse scrapeRunResponse = new ScrapeRunLogResponse();
    scrapeRunResponse.setId(42L);
    scrapeRunResponse.setStatus("SUCCESS");

    IngestionResult ingestionResult = new IngestionResult(42L, List.of(persistentThesis), 12L);

    when(thesisManagementService.replaceThesesInDatabase(eq(chairId), any()))
        .thenReturn(ingestionResult);

    String vectorResponseJson =
        "{\"chairId\":1,\"insertedVectorEntries\":1,\"deletedVectorEntries\":0}";

    mockServer
        .expect(requestTo("/internal/v1/vector-search-service/chairs/1/index"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(vectorResponseJson, MediaType.APPLICATION_JSON));

    // Act
    ChairThesesReplacementResponse response =
        service.executeScrapeIngestionPipeline(chairId, request);

    // Assert
    assertThat(response.getInsertedRelationalTheses()).isEqualTo(1);
    assertThat(response.getReplacedVectorEntries()).isEqualTo(1);
    assertThat(response.getScrapeRunId()).isEqualTo(42L);
    mockServer.verify();
  }
}
