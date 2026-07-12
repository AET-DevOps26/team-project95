package com.project95.thesis.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.project95.thesis.thesis.domain.ResearchArea;
import com.project95.thesis.thesis.repository.ResearchAreaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResearchAreaTaxonomyServiceTest {

  @Mock private ResearchAreaRepository researchAreaRepository;

  @Test
  void canonicalize_SnapsCanonicalAreaCaseInsensitively() {
    ResearchAreaTaxonomyService service = new ResearchAreaTaxonomyService(researchAreaRepository);

    assertThat(service.canonicalize("computer vision")).isEqualTo("Computer Vision");
  }

  @Test
  void listKnownResearchAreasForExtraction_MergesCanonicalAndLinkedDatabaseAreas() {
    when(researchAreaRepository.findDistinctNamesLinkedToTheses())
        .thenReturn(List.of("Custom Area"));
    ResearchAreaTaxonomyService service = new ResearchAreaTaxonomyService(researchAreaRepository);

    assertThat(service.listKnownResearchAreasForExtraction())
        .contains("Computer Vision", "Custom Area");
  }

  @Test
  void isAllowedResearchArea_AllowsExistingDatabaseAreaEvenIfItFailsNewAreaGuardrail() {
    String existingArea = "Very Specific Legacy Area Name That Is Longer Than The New Area Limit";
    when(researchAreaRepository.findByName(existingArea))
        .thenReturn(Optional.of(new ResearchArea(existingArea)));
    ResearchAreaTaxonomyService service = new ResearchAreaTaxonomyService(researchAreaRepository);

    assertThat(service.isAllowedResearchArea(existingArea)).isTrue();
  }

  @Test
  void isAllowedResearchArea_RejectsNoisyUnknownArea() {
    ResearchAreaTaxonomyService service = new ResearchAreaTaxonomyService(researchAreaRepository);

    assertThat(service.isAllowedResearchArea("Thesis project using a product-specific prototype"))
        .isFalse();
  }
}
