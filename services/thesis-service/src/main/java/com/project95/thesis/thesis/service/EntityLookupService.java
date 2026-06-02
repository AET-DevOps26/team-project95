package com.project95.thesis.thesis.service;

import static com.project95.thesis.thesis.utils.Utils.normalize;
import static com.project95.thesis.thesis.utils.Utils.unwrap;

import com.project95.thesis.management.dto.AdvisorInputDto;
import com.project95.thesis.management.dto.SourceEndpointThesesReplacementRequestDto;
import com.project95.thesis.management.dto.ThesisProposalInputDto;
import com.project95.thesis.thesis.domain.Advisor;
import com.project95.thesis.thesis.domain.ResearchArea;
import com.project95.thesis.thesis.domain.Tag;
import com.project95.thesis.thesis.repository.AdvisorRepository;
import com.project95.thesis.thesis.repository.ResearchAreaRepository;
import com.project95.thesis.thesis.repository.TagRepository;
import com.project95.thesis.thesis.utils.Utils;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
   * Synchronizes shared entities (Tags, Research Areas, Advisors) with the database. Performs batch
   * inserts for improved performance.
   */
  @Transactional
  public void ensureSharedEntitiesExist(SourceEndpointThesesReplacementRequestDto request) {
    Set<String> tagNames = new HashSet<>();
    Set<String> areaNames = new HashSet<>();
    Map<String, AdvisorInputDto> advisorByEmail = new HashMap<>();

    for (ThesisProposalInputDto thesis : request.getTheses()) {
      if (thesis.getTags() != null) {
        thesis.getTags().stream()
            .map(Utils::normalize)
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
          if (email != null && !email.isBlank()) {
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
    Set<String> existing =
        tagRepository.findAllByNameIn(names).stream().map(Tag::getName).collect(Collectors.toSet());

    List<Tag> newEntities =
        names.stream()
            .filter(name -> !existing.contains(name))
            .map(Tag::new)
            .collect(Collectors.toList());

    if (!newEntities.isEmpty()) {
      tagRepository.saveAll(newEntities);
    }
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
