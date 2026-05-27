package com.project95.thesis.thesis.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.project95.thesis.management.dto.AdvisorInput;
import com.project95.thesis.management.dto.ChairThesesReplacementRequest;
import com.project95.thesis.management.dto.ThesisProposalInput;
import com.project95.thesis.thesis.domain.Advisor;
import com.project95.thesis.thesis.domain.ResearchArea;
import com.project95.thesis.thesis.domain.Tag;
import com.project95.thesis.thesis.repository.AdvisorRepository;
import com.project95.thesis.thesis.repository.ResearchAreaRepository;
import com.project95.thesis.thesis.repository.TagRepository;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class EntityLookupServiceTest {

  @Mock private TagRepository tagRepository;
  @Mock private ResearchAreaRepository researchAreaRepository;
  @Mock private AdvisorRepository advisorRepository;

  @InjectMocks private EntityLookupService service;

  @Test
  void ensureSharedEntitiesExist_HandlesRaceCondition() {
    // Arrange
    ChairThesesReplacementRequest request = new ChairThesesReplacementRequest();
    ThesisProposalInput input = new ThesisProposalInput();
    input.setTags(List.of("NewTag"));
    request.setTheses(List.of(input));

    // Simulate tag does not exist initially
    when(tagRepository.findAllByNameIn(any())).thenReturn(Collections.emptyList());
    // Simulate another thread inserts it between findAll and save
    when(tagRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Duplicate"));

    // Act
    service.ensureSharedEntitiesExist(request);

    // Assert
    verify(tagRepository).saveAndFlush(any(Tag.class));
    // Should not throw exception
  }

  @Test
  void ensureSharedEntitiesExist_SavesNewEntities() {
    // Arrange
    ChairThesesReplacementRequest request = new ChairThesesReplacementRequest();
    ThesisProposalInput input = new ThesisProposalInput();
    input.setTags(List.of("T1"));
    input.setResearchArea(JsonNullable.of("A1"));
    
    AdvisorInput adv = new AdvisorInput();
    adv.setName("Adv1");
    adv.setEmail(JsonNullable.of("adv1@example.com"));
    input.setAdvisors(List.of(adv));
    
    request.setTheses(List.of(input));

    when(tagRepository.findAllByNameIn(any())).thenReturn(Collections.emptyList());
    when(researchAreaRepository.findAllByNameIn(any())).thenReturn(Collections.emptyList());
    when(advisorRepository.findAllByEmailIn(any())).thenReturn(Collections.emptyList());

    // Act
    service.ensureSharedEntitiesExist(request);

    // Assert
    verify(tagRepository).saveAndFlush(any(Tag.class));
    verify(researchAreaRepository).saveAndFlush(any(ResearchArea.class));
    verify(advisorRepository).saveAndFlush(any(Advisor.class));
  }
}
