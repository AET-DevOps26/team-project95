package com.project95.thesis.thesis.service;

import com.project95.thesis.management.dto.ChairThesesReplacementRequest;
import com.project95.thesis.management.dto.ThesisProposalInput;
import com.project95.thesis.thesis.domain.*;
import com.project95.thesis.thesis.repository.*;
import org.openapitools.jackson.nullable.JsonNullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ThesisManagementService {
  private static final Logger log = LoggerFactory.getLogger(ThesisManagementService.class);

  private final ThesisProposalRepository thesisRepository;
  private final ChairRepository chairRepository;
  private final TagRepository tagRepository;
  private final AdvisorRepository advisorRepository;
  private final ResearchAreaRepository researchAreaRepository;
  private final EntityLookupService entityLookupService;

  public ThesisManagementService(
          ThesisProposalRepository thesisRepository,
          ChairRepository chairRepository,
          TagRepository tagRepository,
          AdvisorRepository advisorRepository,
          ResearchAreaRepository researchAreaRepository,
          EntityLookupService entityLookupService) {
      this.thesisRepository = thesisRepository;
      this.chairRepository = chairRepository;
      this.tagRepository = tagRepository;
      this.advisorRepository = advisorRepository;
      this.researchAreaRepository = researchAreaRepository;
      this.entityLookupService = entityLookupService;
  }

  @Transactional
  public List<ThesisProposal> replaceThesesInDatabase(Long chairId, ChairThesesReplacementRequest request) {
    Objects.requireNonNull(chairId, "chairId must not be null");
    Objects.requireNonNull(request, "request must not be null");

    log.info("Starting atomic database replacement transaction for chairId: {}", chairId);

    Chair chair = chairRepository.findById(chairId)
      .orElseThrow(() -> new IllegalArgumentException("Chair not found with ID: " + chairId));

    thesisRepository.deleteByChairId(chairId);
    thesisRepository.flush();

    if (request.getTheses() == null || request.getTheses().isEmpty()) {
      return Collections.emptyList();
    }

    // 1. Ensure all shared entities exist (handles race conditions with REQUIRES_NEW)
    entityLookupService.ensureSharedEntitiesExist(request);

    // 2. Pre-fetch all shared entities for this transaction to avoid N+1
    Set<String> allTagNames = new HashSet<>();
    Set<String> allAreaNames = new HashSet<>();
    Set<String> allAdvisorEmails = new HashSet<>();
    for (ThesisProposalInput dto : request.getTheses()) {
      if (dto.getTags() != null) allTagNames.addAll(dto.getTags());
      String area = unwrap(dto.getResearchArea());
      if (area != null) allAreaNames.add(area);
      if (dto.getAdvisors() != null) {
        dto.getAdvisors().forEach(a -> {
          String email = unwrap(a.getEmail());
          if (email != null) allAdvisorEmails.add(email);
        });
      }
    }

    Map<String, Tag> tagMap = tagRepository.findAllByNameIn(allTagNames).stream()
        .collect(Collectors.toMap(Tag::getName, t -> t));
    Map<String, ResearchArea> areaMap = researchAreaRepository.findAllByNameIn(allAreaNames).stream()
        .collect(Collectors.toMap(ResearchArea::getName, a -> a));
    Map<String, Advisor> advisorMap = advisorRepository.findAllByEmailIn(allAdvisorEmails).stream()
        .collect(Collectors.toMap(Advisor::getEmail, a -> a));

    List<ThesisProposal> entityList = new ArrayList<>();
    for (ThesisProposalInput dto : request.getTheses()) {
      if (dto.getTitle() == null || dto.getTitle().isBlank()) {
        throw new IllegalArgumentException("Thesis title must not be null or empty");
      }
      if (dto.getSourceUrl() == null) {
        throw new IllegalArgumentException("Thesis source URL must not be null");
      }

      ThesisProposal thesis = new ThesisProposal();
      thesis.setChair(chair);
      thesis.setTitle(dto.getTitle());
      
      thesis.setDegreeType(unwrap(dto.getDegreeType()));
      thesis.setOriginalDescription(unwrap(dto.getOriginalDescription()));
      thesis.setAiOverview(unwrap(dto.getAiOverview()));
      
      thesis.setSourceUrl(dto.getSourceUrl().toString());
      thesis.setStatus(dto.getStatus() != null ? dto.getStatus() : "OPEN");
      thesis.setLastSeenAt(OffsetDateTime.now());

      if (dto.getTags() != null) {
        Set<Tag> managedTags = dto.getTags().stream()
          .map(tagMap::get)
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
        thesis.setTags(managedTags);
      }

      if (dto.getAdvisors() != null) {
        Set<Advisor> managedAdvisors = dto.getAdvisors().stream()
          .map(advDto -> {
            String email = unwrap(advDto.getEmail());
            if (email != null) {
              return advisorMap.get(email);
            }
            // Advisors without email were created/persisted in EntityLookupService
            // but we don't have an easy way to batch-find them here without email.
            // For now, we search by name (not ideal but better than nothing).
            // Actually, EntityLookupService just saved them, they are in the DB.
            return advisorRepository.save(new Advisor(
                advDto.getName(), 
                null, 
                advDto.getProfileUrl() != null ? advDto.getProfileUrl().toString() : null));
          })
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
        thesis.setAdvisors(managedAdvisors);
      }

      String researchAreaStr = unwrap(dto.getResearchArea());
      if (researchAreaStr != null) {
        ResearchArea managedArea = areaMap.get(researchAreaStr);
        if (managedArea != null) {
          thesis.setResearchAreas(new HashSet<>(Collections.singletonList(managedArea)));
        }
      }

      entityList.add(thesis);
    }

    return thesisRepository.saveAll(entityList);
  }

  private <T> T unwrap(JsonNullable<T> nullable) {
    return (nullable != null && nullable.isPresent()) ? nullable.get() : null;
  }
}
