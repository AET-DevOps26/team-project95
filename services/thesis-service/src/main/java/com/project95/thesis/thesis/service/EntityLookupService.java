package com.project95.thesis.thesis.service;

import static com.project95.thesis.thesis.utils.Utils.normalize;
import static com.project95.thesis.thesis.utils.Utils.unwrap;

import com.project95.thesis.management.dto.AdvisorInputDto;
import com.project95.thesis.management.dto.SourceEndpointThesesReplacementRequestDto;
import com.project95.thesis.management.dto.ThesisProposalInputDto;
import com.project95.thesis.thesis.domain.Advisor;
import com.project95.thesis.thesis.domain.ResearchArea;
import com.project95.thesis.thesis.repository.AdvisorRepository;
import com.project95.thesis.thesis.repository.ResearchAreaRepository;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntityLookupService {

  private final ResearchAreaRepository researchAreaRepository;
  private final AdvisorRepository advisorRepository;
  private final ResearchAreaTaxonomyService researchAreaTaxonomyService;

  public EntityLookupService(
      ResearchAreaRepository researchAreaRepository,
      AdvisorRepository advisorRepository,
      ResearchAreaTaxonomyService researchAreaTaxonomyService) {
    this.researchAreaRepository = researchAreaRepository;
    this.advisorRepository = advisorRepository;
    this.researchAreaTaxonomyService = researchAreaTaxonomyService;
  }

  /**
   * Synchronizes shared entities (Research Areas, Advisors) with the database. Performs batch
   * inserts for improved performance.
   */
  @Transactional
  public void ensureSharedEntitiesExist(SourceEndpointThesesReplacementRequestDto request) {
    Set<String> areaNames = new HashSet<>();
    Map<String, AdvisorInputDto> advisorByEmail = new HashMap<>();

    for (ThesisProposalInputDto thesis : request.getTheses()) {
      String area =
          researchAreaTaxonomyService.canonicalize(normalize(unwrap(thesis.getResearchArea())));
      if (area != null) {
        areaNames.add(area);
      }
      if (thesis.getAdvisors() != null) {
        for (AdvisorInputDto adv : thesis.getAdvisors()) {
          String email = normalize(adv.getEmail());
          if (email != null && !email.isBlank()) {
            advisorByEmail.put(email, adv);
          }
        }
      }
    }

    syncResearchAreas(areaNames);
    syncAdvisors(advisorByEmail);
  }

  private void syncResearchAreas(Set<String> names) {
    if (names.isEmpty()) return;
    Set<String> existing =
        researchAreaRepository.findAllByNameIn(names).stream()
            .map(ResearchArea::getName)
            .collect(Collectors.toSet());

    List<ResearchArea> newEntities =
        names.stream()
            .filter(name -> !existing.contains(name))
            .filter(researchAreaTaxonomyService::isAllowedResearchArea)
            .map(ResearchArea::new)
            .collect(Collectors.toList());

    if (!newEntities.isEmpty()) {
      researchAreaRepository.saveAll(newEntities);
    }
  }

  private void syncAdvisors(Map<String, AdvisorInputDto> byEmail) {
    if (byEmail.isEmpty()) return;

    Set<String> existingEmails =
        advisorRepository.findAllByEmailIn(byEmail.keySet()).stream()
            .map(Advisor::getEmail)
            .collect(Collectors.toSet());

    List<Advisor> newEntities =
        byEmail.entrySet().stream()
            .filter(entry -> !existingEmails.contains(entry.getKey()))
            .map(
                entry -> {
                  AdvisorInputDto input = entry.getValue();
                  return new Advisor(
                      input.getName().trim(),
                      normalize(input.getEmail()),
                      input.getProfileUrl() != null
                          ? input.getProfileUrl().toString().trim()
                          : null);
                })
            .collect(Collectors.toList());

    if (!newEntities.isEmpty()) {
      advisorRepository.saveAll(newEntities);
    }
  }
}
