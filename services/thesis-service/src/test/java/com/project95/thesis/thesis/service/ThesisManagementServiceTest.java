package com.project95.thesis.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

import com.project95.thesis.management.dto.AdvisorInputDto;
import com.project95.thesis.management.dto.SourceEndpointThesesReplacementRequestDto;
import com.project95.thesis.management.dto.ThesisProposalInputDto;
import com.project95.thesis.thesis.domain.Advisor;
import com.project95.thesis.thesis.domain.Chair;
import com.project95.thesis.thesis.domain.ResearchArea;
import com.project95.thesis.thesis.domain.SourceEndpoint;
import com.project95.thesis.thesis.domain.Tag;
import com.project95.thesis.thesis.repository.*;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ThesisManagementServiceTest {

  @Mock private ThesisProposalRepository thesisRepository;
  @Mock private SourceEndpointRepository sourceEndpointRepository;
  @Mock private ChairRepository chairRepository;
  @Mock private TagRepository tagRepository;
  @Mock private AdvisorRepository advisorRepository;
  @Mock private ResearchAreaRepository researchAreaRepository;
  @Mock private EntityLookupService entityLookupService;

  @InjectMocks private ThesisManagementService service;

  private Chair testChair;
  private SourceEndpoint testSourceEndpoint;

  @BeforeEach
  void setUp() {
    testChair = new Chair();
    testChair.setId(1L);
    testSourceEndpoint = new SourceEndpoint();
    testSourceEndpoint.setId(1L);
    testSourceEndpoint.setChair(testChair);
  }

  @Test
  void replaceThesesInDatabase_Success() {
    // Arrange
    Long sourceEndpointId = 1L;
    when(sourceEndpointRepository.findById(sourceEndpointId))
        .thenReturn(Optional.of(testSourceEndpoint));

    ThesisProposalInputDto input = new ThesisProposalInputDto();
    input.setTitle("AI in Medicine");
    input.setSourceUrl(URI.create("http://example.com/thesis"));
    input.setDegreeType("MASTER");
    input.setTags(List.of("AI", "Medicine"));

    SourceEndpointThesesReplacementRequestDto request =
        new SourceEndpointThesesReplacementRequestDto();
    request.setTheses(List.of(input));

    Tag tagAi = new Tag("AI");
    Tag tagMed = new Tag("Medicine");
    when(tagRepository.findAllByNameIn(anySet())).thenReturn(List.of(tagAi, tagMed));

    // Return the items being saved
    when(thesisRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
    when(thesisRepository.deleteBySourceEndpointId(sourceEndpointId)).thenReturn(0L);

    // Act
    IngestionResult result = service.replaceThesesInDatabase(sourceEndpointId, request);

    // Assert
    assertThat(result.persistentTheses()).hasSize(1);
    assertThat(result.persistentTheses().get(0).getTitle()).isEqualTo("AI in Medicine");
    assertThat(result.persistentTheses().get(0).getTags()).containsExactlyInAnyOrder(tagAi, tagMed);
    assertThat(result.deletedCount()).isZero();

    verify(entityLookupService).ensureSharedEntitiesExist(request);
    verify(thesisRepository).deleteBySourceEndpointId(sourceEndpointId);
    verify(thesisRepository).saveAll(anyList());
  }

  @Test
  void replaceThesesInDatabase_DeDuplicatesTagsAndAreas() {
    // Arrange
    Long sourceEndpointId = 1L;
    when(sourceEndpointRepository.findById(sourceEndpointId))
        .thenReturn(Optional.of(testSourceEndpoint));

    ThesisProposalInputDto input1 = new ThesisProposalInputDto();
    input1.setTitle(" T1 "); // Add whitespace to test normalization
    input1.setSourceUrl(URI.create("http://u1"));
    input1.setTags(List.of("SharedTag"));
    input1.setResearchArea("SharedArea");
    AdvisorInputDto advisor1 = new AdvisorInputDto();
    advisor1.setName("Shared Advisor");
    advisor1.setEmail("advisor@example.com");
    input1.setAdvisors(List.of(advisor1));

    ThesisProposalInputDto input2 = new ThesisProposalInputDto();
    input2.setTitle("T2");
    input2.setSourceUrl(URI.create("http://u2"));
    input2.setTags(List.of("SharedTag"));
    input2.setResearchArea("SharedArea");
    AdvisorInputDto advisor2 = new AdvisorInputDto();
    advisor2.setName("Shared Advisor");
    advisor2.setEmail("advisor@example.com");
    input2.setAdvisors(List.of(advisor2));

    SourceEndpointThesesReplacementRequestDto request =
        new SourceEndpointThesesReplacementRequestDto();
    request.setTheses(List.of(input1, input2));

    Tag sharedTag = new Tag("SharedTag");
    ResearchArea sharedArea = new ResearchArea("SharedArea");
    Advisor sharedAdvisor = new Advisor("Shared Advisor", "advisor@example.com", null);

    when(tagRepository.findAllByNameIn(anySet())).thenReturn(List.of(sharedTag));
    when(researchAreaRepository.findAllByNameIn(anySet())).thenReturn(List.of(sharedArea));
    when(advisorRepository.findAllByEmailIn(anySet())).thenReturn(List.of(sharedAdvisor));
    when(thesisRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

    when(thesisRepository.deleteBySourceEndpointId(sourceEndpointId)).thenReturn(5L);

    // Act
    IngestionResult result = service.replaceThesesInDatabase(sourceEndpointId, request);

    // Assert
    assertThat(result.persistentTheses()).hasSize(2);
    assertThat(result.persistentTheses().get(0).getTitle())
        .isEqualTo("T1"); // Verified normalization
    assertThat(result.persistentTheses().get(0).getTags()).contains(sharedTag);
    assertThat(result.persistentTheses().get(1).getTags()).contains(sharedTag);
    assertThat(result.persistentTheses().get(0).getResearchAreas()).contains(sharedArea);
    assertThat(result.persistentTheses().get(1).getResearchAreas()).contains(sharedArea);
    assertThat(result.persistentTheses().get(0).getAdvisors()).contains(sharedAdvisor);
    assertThat(result.persistentTheses().get(1).getAdvisors()).contains(sharedAdvisor);
    assertThat(result.deletedCount()).isEqualTo(5L);

    // Verify batch fetching was called with correct sets
    verify(tagRepository).findAllByNameIn(Set.of("SharedTag"));
    verify(researchAreaRepository).findAllByNameIn(Set.of("SharedArea"));
    verify(advisorRepository).findAllByEmailIn(Set.of("advisor@example.com"));
  }

  @Test
  void replaceThesesInDatabase_EmptyRequestDeletesExistingThesesAndSavesNone() {
    Long sourceEndpointId = 1L;
    when(sourceEndpointRepository.findById(sourceEndpointId))
        .thenReturn(Optional.of(testSourceEndpoint));
    when(thesisRepository.deleteBySourceEndpointId(sourceEndpointId)).thenReturn(3L);
    when(thesisRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

    SourceEndpointThesesReplacementRequestDto request =
        new SourceEndpointThesesReplacementRequestDto();
    request.setTheses(List.of());

    IngestionResult result = service.replaceThesesInDatabase(sourceEndpointId, request);

    assertThat(result.persistentTheses()).isEmpty();
    assertThat(result.deletedCount()).isEqualTo(3L);
    verify(entityLookupService).ensureSharedEntitiesExist(request);
    verify(thesisRepository).deleteBySourceEndpointId(sourceEndpointId);
    verify(thesisRepository).saveAll(List.of());
  }

  @Test
  void replaceThesesInDatabase_PreservesDuplicateInputsConsistently() {
    Long sourceEndpointId = 1L;
    when(sourceEndpointRepository.findById(sourceEndpointId))
        .thenReturn(Optional.of(testSourceEndpoint));
    when(thesisRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

    ThesisProposalInputDto first = duplicateInput();
    ThesisProposalInputDto second = duplicateInput();
    SourceEndpointThesesReplacementRequestDto request =
        new SourceEndpointThesesReplacementRequestDto();
    request.setTheses(List.of(first, second));

    IngestionResult result = service.replaceThesesInDatabase(sourceEndpointId, request);

    assertThat(result.persistentTheses()).hasSize(2);
    assertThat(result.persistentTheses())
        .extracting("title")
        .containsExactly("Duplicate Thesis", "Duplicate Thesis");
    verify(thesisRepository)
        .saveAll(
            argThat(
                theses -> {
                  int count = 0;
                  for (var ignored : theses) {
                    count++;
                  }
                  return count == 2;
                }));
  }

  @Test
  void replaceThesesInDatabase_ThrowsIfSourceEndpointNotFound() {
    // Arrange
    Long sourceEndpointId = 99L;
    when(sourceEndpointRepository.findById(sourceEndpointId)).thenReturn(Optional.empty());

    ThesisProposalInputDto input = new ThesisProposalInputDto();
    input.setTitle("Valid Title");
    input.setSourceUrl(URI.create("http://example.com"));

    SourceEndpointThesesReplacementRequestDto request =
        new SourceEndpointThesesReplacementRequestDto();
    request.setTheses(List.of(input));

    // Act & Assert
    assertThrows(
        IllegalArgumentException.class,
        () -> service.replaceThesesInDatabase(sourceEndpointId, request));
    verify(thesisRepository, never()).deleteBySourceEndpointId(any());
  }

  private static ThesisProposalInputDto duplicateInput() {
    ThesisProposalInputDto input = new ThesisProposalInputDto();
    input.setTitle("Duplicate Thesis");
    input.setSourceUrl(URI.create("https://example.com/duplicate"));
    input.setDegreeType("MASTER");
    input.setStatus("OPEN");
    return input;
  }
}

