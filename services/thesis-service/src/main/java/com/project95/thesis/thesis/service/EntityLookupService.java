package com.project95.thesis.thesis.service;

import com.project95.thesis.management.dto.AdvisorInput;
import com.project95.thesis.management.dto.ChairThesesReplacementRequest;
import com.project95.thesis.management.dto.ThesisProposalInput;
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
  public void ensureSharedEntitiesExist(ChairThesesReplacementRequest request) {
    Set<String> tagNames = new HashSet<>();
    Set<String> areaNames = new HashSet<>();
    Map<String, AdvisorInput> advisorByEmail = new HashMap<>();
    List<AdvisorInput> advisorsWithoutEmail = new ArrayList<>();

    for (ThesisProposalInput thesis : request.getTheses()) {
      if (thesis.getTags() != null) {
        tagNames.addAll(thesis.getTags());
      }
      String area = unwrap(thesis.getResearchArea());
      if (area != null) {
        areaNames.add(area);
      }
      if (thesis.getAdvisors() != null) {
        for (AdvisorInput adv : thesis.getAdvisors()) {
          String email = unwrap(adv.getEmail());
          if (email != null) {
            advisorByEmail.put(email, adv);
          } else {
            advisorsWithoutEmail.add(adv);
          }
        }
      }
    }

    syncTags(tagNames);
    syncResearchAreas(areaNames);
    syncAdvisors(advisorByEmail, advisorsWithoutEmail);
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
          // Already created by another thread, ignore
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
          // Already created by another thread, ignore
        }
      }
    }
  }

  private void syncAdvisors(Map<String, AdvisorInput> byEmail, List<AdvisorInput> withoutEmail) {
    if (!byEmail.isEmpty()) {
      Set<String> existingEmails = advisorRepository.findAllByEmailIn(byEmail.keySet()).stream()
          .map(Advisor::getEmail)
          .collect(Collectors.toSet());

      for (Map.Entry<String, AdvisorInput> entry : byEmail.entrySet()) {
        if (!existingEmails.contains(entry.getKey())) {
          AdvisorInput input = entry.getValue();
          try {
            advisorRepository.saveAndFlush(new Advisor(
                input.getName(),
                unwrap(input.getEmail()),
                input.getProfileUrl() != null ? input.getProfileUrl().toString() : null
            ));
          } catch (DataIntegrityViolationException e) {
            // Already created by another thread
          }
        }
      }
    }

    // For advisors without email, we don't have a unique key to de-duplicate,
    // so we just save them. They are likely specific to this thesis or chair.
    // However, the schema now allows NULL email, but unique constraint still exists.
    // Postgres allows multiple NULLs in unique column.
    for (AdvisorInput input : withoutEmail) {
       advisorRepository.save(new Advisor(
           input.getName(),
           null,
           input.getProfileUrl() != null ? input.getProfileUrl().toString() : null
       ));
    }
  }

  private <T> T unwrap(org.openapitools.jackson.nullable.JsonNullable<T> nullable) {
    return (nullable != null && nullable.isPresent()) ? nullable.get() : null;
  }
}
