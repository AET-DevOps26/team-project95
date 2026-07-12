package com.project95.thesis.thesis.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.project95.thesis.management.dto.AdvisorInputDto;
import com.project95.thesis.management.dto.SourceEndpointThesesReplacementRequestDto;
import com.project95.thesis.management.dto.ThesisProposalInputDto;
import com.project95.thesis.thesis.repository.AdvisorRepository;
import com.project95.thesis.thesis.repository.ResearchAreaRepository;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntityLookupServiceTest {

  @Mock private ResearchAreaRepository researchAreaRepository;
  @Mock private AdvisorRepository advisorRepository;
  @Mock private ResearchAreaTaxonomyService researchAreaTaxonomyService;

  @InjectMocks private EntityLookupService service;

  @Test
  void ensureSharedEntitiesExist_SavesNewEntities() {
    // Arrange
    SourceEndpointThesesReplacementRequestDto request =
        new SourceEndpointThesesReplacementRequestDto();
    ThesisProposalInputDto input = new ThesisProposalInputDto();
    input.setResearchArea("Robotics");

    AdvisorInputDto adv = new AdvisorInputDto();
    adv.setName("Adv1");
    adv.setEmail("adv1@example.com");
    input.setAdvisors(List.of(adv));

    request.setTheses(List.of(input));

    when(researchAreaTaxonomyService.canonicalize("Robotics")).thenReturn("Robotics");
    when(researchAreaTaxonomyService.isAllowedResearchArea("Robotics")).thenReturn(true);
    when(researchAreaRepository.findAllByNameIn(any())).thenReturn(Collections.emptyList());
    when(advisorRepository.findAllByEmailIn(any())).thenReturn(Collections.emptyList());

    // Act
    service.ensureSharedEntitiesExist(request);

    // Assert
    verify(researchAreaRepository).saveAll(anyList());
    verify(advisorRepository).saveAll(anyList());
  }
}
