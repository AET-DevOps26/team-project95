package com.project95.thesis.thesis.service;

import com.project95.thesis.management.dto.ChairThesesReplacementRequest;
import com.project95.thesis.management.dto.ScrapeRunLogRequest;
import com.project95.thesis.management.dto.ScrapeRunLogResponse;
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
  private final ScrapeRunService scrapeRunService;

  public ThesisManagementService(
          ThesisProposalRepository thesisRepository,
          ChairRepository chairRepository,
          TagRepository tagRepository,
          AdvisorRepository advisorRepository,
          ResearchAreaRepository researchAreaRepository,
          EntityLookupService entityLookupService,
          ScrapeRunService scrapeRunService) {
      this.thesisRepository = thesisRepository;
      this.chairRepository = chairRepository;
      this.tagRepository = tagRepository;
      this.advisorRepository = advisorRepository;
      this.researchAreaRepository = researchAreaRepository;
      this.entityLookupService = entityLookupService;
      this.scrapeRunService = scrapeRunService;
  }

  @Transactional
  public IngestionResult replaceThesesInDatabase(Long chairId, ChairThesesReplacementRequest request) {
    Objects.requireNonNull(chairId, "chairId must not be null");
    Objects.requireNonNull(request, "request must not be null");

    log.info("Starting atomic database replacement transaction for chairId: {}", chairId);

    Chair chair = chairRepository.findById(chairId)
      .orElseThrow(() -> new IllegalArgumentException("Chair not found with ID: " + chairId));

    long deletedCount = thesisRepository.deleteByChairId(chairId);
    thesisRepository.flush();

    List<ThesisProposal> entityList = new ArrayList<>();
    if (request.getTheses() != null && !request.getTheses().isEmpty()) {
      // 1. Ensure all shared entities exist (handles race conditions with REQUIRES_NEW)
      entityLookupService.ensureSharedEntitiesExist(request);

      // 2. Pre-fetch all shared entities for this transaction to avoid N+1
      Set<String> allTagNames = new HashSet<>();
      Set<String> allAreaNames = new HashSet<>();
      Set<String> allAdvisorEmails = new HashSet<>();
      for (ThesisProposalInput dto : request.getTheses()) {
        if (dto.getTags() != null) {
          dto.getTags().stream()
              .map(this::normalize)
              .filter(Objects::nonNull)
              .forEach(allTagNames::add);
        }
        String area = normalize(unwrap(dto.getResearchArea()));
        if (area != null) allAreaNames.add(area);
        
        if (dto.getAdvisors() != null) {
          dto.getAdvisors().forEach(a -> {
            String email = normalize(a.getEmail());
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

      for (ThesisProposalInput dto : request.getTheses()) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
          throw new IllegalArgumentException("Thesis title must not be null or empty");
        }
        if (dto.getSourceUrl() == null) {
          throw new IllegalArgumentException("Thesis source URL must not be null");
        }

        ThesisProposal thesis = new ThesisProposal();
        thesis.setChair(chair);
        thesis.setTitle(dto.getTitle().trim());
        
        thesis.setDegreeType(normalize(unwrap(dto.getDegreeType())));
        thesis.setOriginalDescription(unwrap(dto.getOriginalDescription()));
        thesis.setAiOverview(unwrap(dto.getAiOverview()));
        
        thesis.setSourceUrl(dto.getSourceUrl().toString());
        thesis.setStatus(dto.getStatus() != null ? dto.getStatus() : "OPEN");
        thesis.setLastSeenAt(OffsetDateTime.now());

        if (dto.getTags() != null) {
          Set<Tag> managedTags = dto.getTags().stream()
            .map(this::normalize)
            .map(tagMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
          thesis.setTags(managedTags);
        }

        if (dto.getAdvisors() != null) {
          Set<Advisor> managedAdvisors = dto.getAdvisors().stream()
            .map(advDto -> {
              String email = normalize(advDto.getEmail());
              return (email != null) ? advisorMap.get(email) : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
          thesis.setAdvisors(managedAdvisors);
        }

        String researchAreaStr = normalize(unwrap(dto.getResearchArea()));
        if (researchAreaStr != null) {
          ResearchArea managedArea = areaMap.get(researchAreaStr);
          if (managedArea != null) {
            thesis.setResearchAreas(new HashSet<>(Collections.singletonList(managedArea)));
          }
        }

        entityList.add(thesis);
      }
      entityList = thesisRepository.saveAll(entityList);
    }

    // Log the scrape run result as a persisted entity (atomic with the replacement)
    if (request.getStatus() == null) {
      throw new IllegalArgumentException("Replacement request status must not be null");
    }

    ScrapeRunLogRequest logRequest = new ScrapeRunLogRequest();
    logRequest.setSourceEndpointId(request.getSourceEndpointId());
    logRequest.setStartedAt(request.getStartedAt());
    logRequest.setFinishedAt(request.getFinishedAt());
    logRequest.setStatus(ScrapeRunLogRequest.StatusEnum.valueOf(request.getStatus().getValue()));
    logRequest.setErrorMessage(request.getErrorMessage());
    logRequest.setCandidatesFound(JsonNullable.of(entityList.size()));

    ScrapeRunLogResponse logResponse = scrapeRunService.logScrapeRun(logRequest);

    return new IngestionResult(logResponse.getId(), entityList, deletedCount);
  }

  private String normalize(String input) {
    if (input == null || input.isBlank()) return null;
    return input.trim();
  }

  private <T> T unwrap(JsonNullable<T> nullable) {
    return (nullable != null && nullable.isPresent()) ? nullable.get() : null;
  }
}
