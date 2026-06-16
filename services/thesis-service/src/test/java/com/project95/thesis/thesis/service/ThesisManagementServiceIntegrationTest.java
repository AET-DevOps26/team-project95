package com.project95.thesis.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.project95.thesis.management.dto.SourceEndpointThesesReplacementRequestDto;
import com.project95.thesis.management.dto.ThesisProposalInputDto;
import com.project95.thesis.thesis.domain.Chair;
import com.project95.thesis.thesis.domain.SourceEndpoint;
import com.project95.thesis.thesis.domain.ThesisProposal;
import com.project95.thesis.thesis.repository.AdvisorRepository;
import com.project95.thesis.thesis.repository.ChairRepository;
import com.project95.thesis.thesis.repository.ResearchAreaRepository;
import com.project95.thesis.thesis.repository.SourceEndpointRepository;
import com.project95.thesis.thesis.repository.TagRepository;
import com.project95.thesis.thesis.repository.ThesisProposalRepository;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ThesisManagementServiceIntegrationTest {

  @Autowired private ThesisManagementService thesisManagementService;
  @Autowired private ThesisProposalRepository thesisRepository;
  @Autowired private SourceEndpointRepository sourceEndpointRepository;
  @Autowired private ChairRepository chairRepository;
  @Autowired private TagRepository tagRepository;
  @Autowired private ResearchAreaRepository researchAreaRepository;
  @Autowired private AdvisorRepository advisorRepository;

  private SourceEndpoint sourceEndpoint;

  @BeforeEach
  void setUp() {
    thesisRepository.deleteAll();
    sourceEndpointRepository.deleteAll();
    chairRepository.deleteAll();
    tagRepository.deleteAll();
    researchAreaRepository.deleteAll();
    advisorRepository.deleteAll();

    Chair chair = chairRepository.save(new Chair("Rollback Chair", "https://chair.example.com"));
    sourceEndpoint = new SourceEndpoint();
    sourceEndpoint.setChair(chair);
    sourceEndpoint.setUrl("https://chair.example.com/theses");
    sourceEndpoint.setStatus("ACTIVE");
    sourceEndpoint = sourceEndpointRepository.save(sourceEndpoint);

    ThesisProposal existing = new ThesisProposal();
    existing.setTitle("Existing Thesis");
    existing.setSourceUrl("https://chair.example.com/existing");
    existing.setStatus("OPEN");
    existing.setChair(chair);
    existing.setSourceEndpoint(sourceEndpoint);
    thesisRepository.save(existing);
  }

  @Test
  void replaceThesesInDatabase_RollsBackDeletedThesesAndSharedEntitiesWhenValidationFails() {
    SourceEndpointThesesReplacementRequestDto request =
        new SourceEndpointThesesReplacementRequestDto();
    ThesisProposalInputDto invalid = new ThesisProposalInputDto();
    invalid.setTitle("   ");
    invalid.setSourceUrl(URI.create("https://chair.example.com/invalid"));
    invalid.setTags(List.of("RollbackTag"));
    invalid.setResearchArea("RollbackArea");
    request.setTheses(List.of(invalid));

    assertThatThrownBy(
            () -> thesisManagementService.replaceThesesInDatabase(sourceEndpoint.getId(), request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Thesis title must not be blank");

    assertThat(thesisRepository.findAll()).extracting(ThesisProposal::getTitle).containsExactly("Existing Thesis");
    assertThat(tagRepository.findAll()).isEmpty();
    assertThat(researchAreaRepository.findAll()).isEmpty();
  }

  @Test
  void replaceThesesInDatabase_RollsBackDeletedThesesWhenSavingNewThesesFails() {
    SourceEndpointThesesReplacementRequestDto request =
        new SourceEndpointThesesReplacementRequestDto();
    ThesisProposalInputDto invalid = new ThesisProposalInputDto();
    invalid.setTitle("x".repeat(300));
    invalid.setSourceUrl(URI.create("https://chair.example.com/title-too-long"));
    request.setTheses(List.of(invalid));

    assertThatThrownBy(
            () -> thesisManagementService.replaceThesesInDatabase(sourceEndpoint.getId(), request))
        .isInstanceOf(RuntimeException.class);

    assertThat(thesisRepository.findAll())
        .extracting(ThesisProposal::getTitle)
        .containsExactly("Existing Thesis");
  }
}
