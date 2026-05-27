package com.project95.thesis.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

import com.project95.thesis.management.dto.ChairThesesReplacementRequest;
import com.project95.thesis.management.dto.ThesisProposalInput;
import com.project95.thesis.thesis.domain.Chair;
import com.project95.thesis.thesis.domain.ResearchArea;
import com.project95.thesis.thesis.domain.Tag;
import com.project95.thesis.thesis.domain.ThesisProposal;
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
import org.openapitools.jackson.nullable.JsonNullable;

@ExtendWith(MockitoExtension.class)
class ThesisManagementServiceTest {

  @Mock private ThesisProposalRepository thesisRepository;
  @Mock private ChairRepository chairRepository;
  @Mock private TagRepository tagRepository;
  @Mock private AdvisorRepository advisorRepository;
  @Mock private ResearchAreaRepository researchAreaRepository;
  @Mock private EntityLookupService entityLookupService;

  @InjectMocks private ThesisManagementService service;

  private Chair testChair;

  @BeforeEach
  void setUp() {
    testChair = new Chair();
    testChair.setId(1L);
  }

  @Test
  void replaceThesesInDatabase_Success() {
    // Arrange
    Long chairId = 1L;
    when(chairRepository.findById(chairId)).thenReturn(Optional.of(testChair));

    ThesisProposalInput input = new ThesisProposalInput();
    input.setTitle("AI in Medicine");
    input.setSourceUrl(URI.create("http://example.com/thesis"));
    input.setDegreeType(JsonNullable.of("MASTER"));
    input.setTags(List.of("AI", "Medicine"));

    ChairThesesReplacementRequest request = new ChairThesesReplacementRequest();
    request.setTheses(List.of(input));

    Tag tagAi = new Tag("AI");
    Tag tagMed = new Tag("Medicine");
    when(tagRepository.findAllByNameIn(anySet())).thenReturn(List.of(tagAi, tagMed));

    // Return the items being saved
    when(thesisRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

    // Act
    List<ThesisProposal> result = service.replaceThesesInDatabase(chairId, request);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("AI in Medicine");
    assertThat(result.get(0).getTags()).containsExactlyInAnyOrder(tagAi, tagMed);

    verify(entityLookupService).ensureSharedEntitiesExist(request);
    verify(thesisRepository).deleteByChairId(chairId);
    verify(thesisRepository).saveAll(anyList());
  }

  @Test
  void replaceThesesInDatabase_DeDuplicatesTagsAndAreas() {
    // Arrange
    Long chairId = 1L;
    when(chairRepository.findById(chairId)).thenReturn(Optional.of(testChair));

    ThesisProposalInput input1 = new ThesisProposalInput();
    input1.setTitle("T1");
    input1.setSourceUrl(URI.create("http://u1"));
    input1.setTags(List.of("SharedTag"));
    input1.setResearchArea(JsonNullable.of("SharedArea"));

    ThesisProposalInput input2 = new ThesisProposalInput();
    input2.setTitle("T2");
    input2.setSourceUrl(URI.create("http://u2"));
    input2.setTags(List.of("SharedTag"));
    input2.setResearchArea(JsonNullable.of("SharedArea"));

    ChairThesesReplacementRequest request = new ChairThesesReplacementRequest();
    request.setTheses(List.of(input1, input2));

    Tag sharedTag = new Tag("SharedTag");
    ResearchArea sharedArea = new ResearchArea("SharedArea");

    when(tagRepository.findAllByNameIn(anySet())).thenReturn(List.of(sharedTag));
    when(researchAreaRepository.findAllByNameIn(anySet())).thenReturn(List.of(sharedArea));
    when(thesisRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

    // Act
    List<ThesisProposal> result = service.replaceThesesInDatabase(chairId, request);

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getTags()).contains(sharedTag);
    assertThat(result.get(1).getTags()).contains(sharedTag);
    assertThat(result.get(0).getResearchAreas()).contains(sharedArea);
    assertThat(result.get(1).getResearchAreas()).contains(sharedArea);

    // Verify batch fetching was called with correct sets
    verify(tagRepository).findAllByNameIn(Set.of("SharedTag"));
    verify(researchAreaRepository).findAllByNameIn(Set.of("SharedArea"));
  }

  @Test
  void replaceThesesInDatabase_ThrowsIfChairNotFound() {
    // Arrange
    Long chairId = 99L;
    when(chairRepository.findById(chairId)).thenReturn(Optional.empty());
    ChairThesesReplacementRequest request = new ChairThesesReplacementRequest();

    // Act & Assert
    assertThrows(
        IllegalArgumentException.class, () -> service.replaceThesesInDatabase(chairId, request));
    verify(thesisRepository, never()).deleteByChairId(any());
  }

  @Test
  void replaceThesesInDatabase_ThrowsIfTitleMissing() {
    // Arrange
    Long chairId = 1L;
    when(chairRepository.findById(chairId)).thenReturn(Optional.of(testChair));

    ThesisProposalInput input = new ThesisProposalInput();
    input.setSourceUrl(URI.create("http://example.com/thesis"));

    ChairThesesReplacementRequest request = new ChairThesesReplacementRequest();
    request.setTheses(List.of(input));

    // Act & Assert
    assertThrows(
        IllegalArgumentException.class, () -> service.replaceThesesInDatabase(chairId, request));
  }

  @Test
  void replaceThesesInDatabase_ThrowsIfSourceUrlMissing() {
    // Arrange
    Long chairId = 1L;
    when(chairRepository.findById(chairId)).thenReturn(Optional.of(testChair));

    ThesisProposalInput input = new ThesisProposalInput();
    input.setTitle("AI in Medicine");

    ChairThesesReplacementRequest request = new ChairThesesReplacementRequest();
    request.setTheses(List.of(input));

    // Act & Assert
    assertThrows(
        IllegalArgumentException.class, () -> service.replaceThesesInDatabase(chairId, request));
  }
}
