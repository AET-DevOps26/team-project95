package com.project95.thesis.thesis.service;

import com.project95.thesis.management.dto.AdvisorInputDto;
import com.project95.thesis.management.dto.ChairThesesReplacementRequestDto;
import com.project95.thesis.management.dto.ThesisProposalInputDto;
import com.project95.thesis.thesis.domain.Advisor;
import com.project95.thesis.thesis.domain.ResearchArea;
import com.project95.thesis.thesis.domain.Tag;
import com.project95.thesis.thesis.repository.AdvisorRepository;
import com.project95.thesis.thesis.repository.ResearchAreaRepository;
import com.project95.thesis.thesis.repository.TagRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EntityLookupService {

  private final TagRepository tagRepository;
  private final ResearchAreaRepository researchAreaRepository;
  private final AdvisorRepository advisorRepository;

  public EntityLookupService(
      TagRepository tagRepository,
      ResearchAreaRepository researchAreaRepository,
      AdvisorRepository advisorRepository) {
    this.tagRepository = tagRepository;
    this.researchAreaRepository = researchAreaRepository;
    this.advisorRepository = advisorRepository;
  }

  /**
   * Synchronizes shared entities (Tags, Research Areas, Advisors) with the database.
   * Uses REQUIRES_NEW to ensure that unique constraint violations during creation
   * do not roll back the main thesis replacement transaction, and to make these
   * entities visible to other concurrent requests.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void ensureSharedEntitiesExist(ChairThesesReplacementRequestDto request) {
    Set<String> tagNames = new HashSet<>();
    Set<String> areaNames = new HashSet<>();
    Map<String, AdvisorInputDto> advisorByEmail = new HashMap<>();

    for (ThesisProposalInputDto thesis : request.getTheses()) {
      if (thesis.getTags() != null) {
        thesis.getTags().stream()
            .map(this::normalize)
            .filter(Objects::nonNull)
            .forEach(tagNames::add);
      }
      String area = normalize(unwrap(thesis.getResearchArea()));
      if (area != null) {
        areaNames.add(area);
      }
      if (thesis.getAdvisors() != null) {
        for (AdvisorInputDto adv : thesis.getAdvisors()) {
          String email = normalize(adv.getEmail());
          if (email != null) {
            advisorByEmail.put(email, adv);
          }
        }
      }
    }

    syncTags(tagNames);
    syncResearchAreas(areaNames);
    syncAdvisors(advisorByEmail);
  }

  private void syncTags(Set<String> names) {
    if (names.isEmpty()) return;
    Set<String> existing = tagRepository.findAllByNameIn(names).stream()
        .map(Tag::getName)
        .collect(Collectors.toSet());

    for (String name : names) {
      if (!existing.contains(name)) {
        try {
          tagRepository.saveAndFlush(new Tag(name));
        } catch (DataIntegrityViolationException e) {
          // Already created by another thread, ignore uniqueness violations
        }
      }
    }
  }

  private void syncResearchAreas(Set<String> names) {
    if (names.isEmpty()) return;
    Set<String> existing = researchAreaRepository.findAllByNameIn(names).stream()
        .map(ResearchArea::getName)
        .collect(Collectors.toSet());

    for (String name : names) {
      if (!existing.contains(name)) {
        try {
          researchAreaRepository.saveAndFlush(new ResearchArea(name));
        } catch (DataIntegrityViolationException e) {
          // Already created by another thread, ignore uniqueness violations
        }
      }
    }
  }

  private void syncAdvisors(Map<String, AdvisorInputDto> byEmail) {
    if (byEmail.isEmpty()) return;
    
    Set<String> existingEmails = advisorRepository.findAllByEmailIn(byEmail.keySet()).stream()
        .map(Advisor::getEmail)
        .collect(Collectors.toSet());

    for (Map.Entry<String, AdvisorInputDto> entry : byEmail.entrySet()) {
      if (!existingEmails.contains(entry.getKey())) {
        AdvisorInputDto input = entry.getValue();
        if (input.getName() == null || input.getName().isBlank()) {
          continue; // Skip advisors without a valid name
        }
        try {
          advisorRepository.saveAndFlush(new Advisor(
              input.getName().trim(),
              normalize(input.getEmail()),
              input.getProfileUrl() != null ? input.getProfileUrl().toString().trim() : null
          ));
        } catch (DataIntegrityViolationException e) {
          // Already created by another thread
        }
      }
    }
  }

  private String normalize(String input) {
    if (input == null || input.isBlank()) return null;
    return input.trim();
  }

  private <T> T unwrap(org.openapitools.jackson.nullable.JsonNullable<T> nullable) {
    return (nullable != null && nullable.isPresent()) ? nullable.get() : null;
  }
}
