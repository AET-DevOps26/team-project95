package com.project95.thesis.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.project95.thesis.management.dto.SourceEndpointThesesReplacementRequestDto;
import com.project95.thesis.management.dto.ThesisProposalInputDto;
import com.project95.thesis.thesis.domain.Chair;
import com.project95.thesis.thesis.domain.SourceEndpoint;
import com.project95.thesis.thesis.domain.ThesisProposal;
import com.project95.thesis.thesis.repository.ChairRepository;
import com.project95.thesis.thesis.repository.SourceEndpointRepository;
import com.project95.thesis.thesis.repository.ThesisProposalRepository;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("flyway-test")
class ThesisIngestionIntegrationTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

  @Autowired private ThesisManagementService thesisManagementService;
  @Autowired private ThesisProposalRepository thesisRepository;
  @Autowired private SourceEndpointRepository sourceEndpointRepository;
  @Autowired private ChairRepository chairRepository;

  private SourceEndpoint endpoint;

  @BeforeEach
  void setUp() {
    thesisRepository.deleteAll();
    sourceEndpointRepository.deleteAll();
    chairRepository.deleteAll();

    Chair chair = chairRepository.save(new Chair("Ingestion Chair", "http://ingestion.tum.de"));
    SourceEndpoint se = new SourceEndpoint();
    se.setChair(chair);
    se.setUrl("http://ingestion.tum.de/theses");
    se.setStatus("ACTIVE");
    endpoint = sourceEndpointRepository.save(se);
  }

  @Test
  void replaceTheses_AtomicSuccess() {
    // 1. Initial state: 1 thesis
    createThesis("Old Thesis");
    assertThat(thesisRepository.count()).isEqualTo(1);

    // 2. Act: Replace with 2 new ones
    SourceEndpointThesesReplacementRequestDto request = new SourceEndpointThesesReplacementRequestDto();
    request.setTheses(List.of(
        createInputDto("New Thesis 1"),
        createInputDto("New Thesis 2")
    ));

    thesisManagementService.replaceThesesInDatabase(endpoint.getId(), request);

    // 3. Assert: Old gone, new here
    assertThat(thesisRepository.count()).isEqualTo(2);
    assertThat(thesisRepository.findAll())
        .extracting(ThesisProposal::getTitle)
        .containsExactlyInAnyOrder("New Thesis 1", "New Thesis 2");
  }

  @Test
  void replaceTheses_RollbackOnFailure() {
    // 1. Initial state: 1 thesis
    createThesis("Old Thesis");
    assertThat(thesisRepository.count()).isEqualTo(1);

    // 2. Act: Attempt replacement but one is invalid (null title)
    SourceEndpointThesesReplacementRequestDto request = new SourceEndpointThesesReplacementRequestDto();
    ThesisProposalInputDto invalid = createInputDto("Valid");
    invalid.setTitle(null); // This will trigger IllegalArgumentException in service

    request.setTheses(List.of(createInputDto("Valid 1"), invalid));

    assertThatThrownBy(() -> thesisManagementService.replaceThesesInDatabase(endpoint.getId(), request))
        .isInstanceOf(IllegalArgumentException.class);

    // 3. Assert: ROLLBACK occurred. Old thesis still exists, no new ones inserted.
    assertThat(thesisRepository.count()).isEqualTo(1);
    assertThat(thesisRepository.findAll().get(0).getTitle()).isEqualTo("Old Thesis");
  }

  @Test
  void replaceTheses_EmptyRequest_DeletesAll() {
    createThesis("To be deleted");
    assertThat(thesisRepository.count()).isEqualTo(1);

    SourceEndpointThesesReplacementRequestDto request = new SourceEndpointThesesReplacementRequestDto();
    request.setTheses(List.of());

    thesisManagementService.replaceThesesInDatabase(endpoint.getId(), request);

    assertThat(thesisRepository.count()).isZero();
  }

  @Test
  @Transactional
  void replaceTheses_SharedEntitiesLinking() {
    SourceEndpointThesesReplacementRequestDto request = new SourceEndpointThesesReplacementRequestDto();
    ThesisProposalInputDto input = createInputDto("Shared Entity Test");
    input.setTags(List.of("Tag1", "Tag2"));
    input.setResearchArea("Area1");
    request.setTheses(List.of(input));

    thesisManagementService.replaceThesesInDatabase(endpoint.getId(), request);

    ThesisProposal saved = thesisRepository.findAll().get(0);
    assertThat(saved.getTags()).hasSize(2);
    assertThat(saved.getResearchAreas()).hasSize(1);
  }


  private void createThesis(String title) {
    ThesisProposal t = new ThesisProposal();
    t.setTitle(title);
    t.setChair(endpoint.getChair());
    t.setSourceEndpoint(endpoint);
    t.setSourceUrl("http://test.com/" + title.replace(" ", ""));
    t.setStatus("OPEN");
    thesisRepository.save(t);
  }

  private ThesisProposalInputDto createInputDto(String title) {
    ThesisProposalInputDto dto = new ThesisProposalInputDto();
    dto.setTitle(title);
    dto.setSourceUrl(URI.create("http://test.com/" + title.replace(" ", "")));
    dto.setStatus("OPEN");
    return dto;
  }
}
