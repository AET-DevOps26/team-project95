package com.project95.thesis.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.project95.thesis.management.dto.SourceEndpointThesesReplacementRequestDto;
import com.project95.thesis.management.dto.SourceEndpointThesesReplacementResponseDto;
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

  private MockRestServiceServer mockServer;
  private ThesisCoordinationService service;

  @BeforeEach
  void setUp() {
    RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("http://vector-service");
    mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

    service =
        new ThesisCoordinationService(
            thesisManagementService,
            restClientBuilder.build(),
            new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
  }

  @Test
  void executeScrapeIngestionPipeline_CallsVectorService() {
    // Arrange
    Long sourceEndpointId = 1L;
    SourceEndpointThesesReplacementRequestDto request =
        new SourceEndpointThesesReplacementRequestDto();

    com.project95.thesis.thesis.domain.Chair chair = new com.project95.thesis.thesis.domain.Chair();
    chair.setId(sourceEndpointId);

    ThesisProposal persistentThesis = new ThesisProposal();
    persistentThesis.setId(100L);
    persistentThesis.setTitle("Vector Sync Test");
    persistentThesis.setChair(chair);

    com.project95.thesis.thesis.domain.SourceEndpoint endpoint =
        new com.project95.thesis.thesis.domain.SourceEndpoint();
    endpoint.setId(sourceEndpointId);
    persistentThesis.setSourceEndpoint(endpoint);

    IngestionResult ingestionResult = new IngestionResult(List.of(persistentThesis), 12L);

    when(thesisManagementService.replaceThesesInDatabase(eq(sourceEndpointId), any()))
        .thenReturn(ingestionResult);

    String vectorResponseJson =
        "{\"chairId\":1,\"insertedVectorEntries\":1,\"deletedVectorEntries\":0}";

    mockServer
        .expect(
            requestTo(
                "http://vector-service/internal/v1/vector-search-service/source-endpoints/1/index"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(vectorResponseJson, MediaType.APPLICATION_JSON));

    request.setLastContentHash("success-hash");

    // Act
    SourceEndpointThesesReplacementResponseDto response =
        service.executeScrapeIngestionPipeline(sourceEndpointId, request);

    // Assert
    assertThat(response.getInsertedRelationalTheses()).isEqualTo(1);
    assertThat(response.getReplacedVectorEntries()).isEqualTo(1);
    verify(thesisManagementService).updateLastContentHash(sourceEndpointId, "success-hash");
    mockServer.verify();
  }

  @Test
  void executeScrapeIngestionPipeline_VectorServiceFailure_DoesNotUpdateContentHash() {
    // Arrange
    Long sourceEndpointId = 1L;
    SourceEndpointThesesReplacementRequestDto request =
        new SourceEndpointThesesReplacementRequestDto();
    request.setLastContentHash("failure-hash");

    com.project95.thesis.thesis.domain.Chair chair = new com.project95.thesis.thesis.domain.Chair();
    chair.setId(sourceEndpointId);

    ThesisProposal persistentThesis = new ThesisProposal();
    persistentThesis.setId(100L);
    persistentThesis.setTitle("Vector Sync Failure Test");
    persistentThesis.setChair(chair);

    com.project95.thesis.thesis.domain.SourceEndpoint endpoint =
        new com.project95.thesis.thesis.domain.SourceEndpoint();
    endpoint.setId(sourceEndpointId);
    persistentThesis.setSourceEndpoint(endpoint);

    IngestionResult ingestionResult = new IngestionResult(List.of(persistentThesis), 12L);

    when(thesisManagementService.replaceThesesInDatabase(eq(sourceEndpointId), any()))
        .thenReturn(ingestionResult);

    mockServer
        .expect(
            requestTo(
                "http://vector-service/internal/v1/vector-search-service/source-endpoints/1/index"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            org.springframework.test.web.client.response.MockRestResponseCreators
                .withServerError());

    // Act
    SourceEndpointThesesReplacementResponseDto response =
        service.executeScrapeIngestionPipeline(sourceEndpointId, request);

    // Assert
    assertThat(response.getInsertedRelationalTheses()).isEqualTo(1);
    assertThat(response.getReplacedVectorEntries()).isEqualTo(0);
    assertThat(response.getErrorMessage()).contains("Vector sync failed");
    verify(thesisManagementService, never()).updateLastContentHash(any(), any());
    mockServer.verify();
  }
}
