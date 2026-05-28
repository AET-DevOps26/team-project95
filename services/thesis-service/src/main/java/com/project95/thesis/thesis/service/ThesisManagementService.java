package com.project95.thesis.thesis.service;

import com.project95.thesis.management.dto.AdvisorInputDto;
import com.project95.thesis.management.dto.ChairThesesReplacementRequestDto;
import com.project95.thesis.management.dto.ScrapeRunLogRequestDto;
import com.project95.thesis.management.dto.ScrapeRunLogResponseDto;
import com.project95.thesis.management.dto.ThesisProposalInputDto;
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
  public IngestionResult replaceThesesInDatabase(Long chairId, ChairThesesReplacementRequestDto request) {
    Objects.requireNonNull(chairId, "chairId must not be null");
    Objects.requireNonNull(request, "request must not be null");

    // 1. Validate required root fields up front
    if (request.getSourceEndpointId() == null) {
      throw new IllegalArgumentException("sourceEndpointId must not be null");
    }
    if (request.getStartedAt() == null) {
      throw new IllegalArgumentException("startedAt must not be null");
    }
    if (request.getFinishedAt() == null) {
      throw new IllegalArgumentException("finishedAt must not be null");
    }
    if (request.getStatus() == null) {
      throw new IllegalArgumentException("status must not be null");
    }

    // 2. Reject empty/null theses to prevent accidental data wipe
    if (request.getTheses() == null || request.getTheses().isEmpty()) {
      throw new IllegalArgumentException("Theses list must not be null or empty");
    }

    // 3. Pre-validate all theses and advisors BEFORE side-effect-heavy ensureSharedEntitiesExist
    for (ThesisProposalInputDto dto : request.getTheses()) {
      if (dto.getTitle() == null || dto.getTitle().isBlank()) {
        throw new IllegalArgumentException("Thesis title must not be null or empty");
      }
      if (dto.getSourceUrl() == null) {
        throw new IllegalArgumentException("Thesis source URL must not be null");
      }
      if (dto.getAdvisors() != null) {
        for (AdvisorInputDto adv : dto.getAdvisors()) {
          if (adv.getName() == null || adv.getName().isBlank()) {
            throw new IllegalArgumentException("Advisor name must not be null or empty");
          }
          if (adv.getEmail() == null || adv.getEmail().isBlank()) {
            throw new IllegalArgumentException("Advisor email must not be null or empty");
          }
        }
      }
    }

    log.info("Starting atomic database replacement transaction for chairId: {}", chairId);

    Chair chair = chairRepository.findById(chairId)
      .orElseThrow(() -> new IllegalArgumentException("Chair not found with ID: " + chairId));

    // 4. Perform side-effect-heavy shared entity synchronization
    entityLookupService.ensureSharedEntitiesExist(request);

    // 5. Atomic deletion and replacement
    long deletedCount = thesisRepository.deleteByChairId(chairId);
    thesisRepository.flush();

    List<ThesisProposal> entityList = new ArrayList<>();
    // 2. Pre-fetch all shared entities for this transaction to avoid N+1
    Set<String> allTagNames = new HashSet<>();
    Set<String> allAreaNames = new HashSet<>();
    Set<String> allAdvisorEmails = new HashSet<>();
    for (ThesisProposalInputDto dto : request.getTheses()) {
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

    for (ThesisProposalInputDto dto : request.getTheses()) {
      ThesisProposal thesis = new ThesisProposal();
      thesis.setChair(chair);
      thesis.setTitle(dto.getTitle().trim());
      
      thesis.setDegreeType(normalize(unwrap(dto.getDegreeType())));
      thesis.setOriginalDescription(unwrap(dto.getOriginalDescription()));
      thesis.setAiOverview(unwrap(dto.getAiOverview()));
      
      thesis.setSourceUrl(dto.getSourceUrl().toString());
      
      String normalizedStatus = normalize(dto.getStatus());
      thesis.setStatus(normalizedStatus != null ? normalizedStatus : "OPEN");
      
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

    // Log the scrape run result as a persisted entity (atomic with the replacement)
    ScrapeRunLogRequestDto logRequest = new ScrapeRunLogRequestDto();
    logRequest.setSourceEndpointId(request.getSourceEndpointId());
    logRequest.setStartedAt(request.getStartedAt());
    logRequest.setFinishedAt(request.getFinishedAt());
    logRequest.setStatus(ScrapeRunLogRequestDto.StatusEnum.valueOf(request.getStatus().getValue()));
    logRequest.setErrorMessage(request.getErrorMessage());
    logRequest.setCandidatesFound(JsonNullable.of(entityList.size()));

    ScrapeRunLogResponseDto logResponse = scrapeRunService.logScrapeRun(logRequest);

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
