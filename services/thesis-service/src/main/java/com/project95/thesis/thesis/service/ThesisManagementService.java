package com.project95.thesis.thesis.service;

import static com.project95.thesis.thesis.utils.Utils.normalize;
import static com.project95.thesis.thesis.utils.Utils.unwrap;

import com.project95.thesis.management.dto.SourceEndpointThesesReplacementRequestDto;
import com.project95.thesis.management.dto.ThesisProposalInputDto;
import com.project95.thesis.thesis.domain.*;
import com.project95.thesis.thesis.repository.*;
import com.project95.thesis.thesis.utils.Utils;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ThesisManagementService {
  private static final Logger log = LoggerFactory.getLogger(ThesisManagementService.class);

  private final ThesisProposalRepository thesisRepository;
  private final ChairRepository chairRepository;
  private final SourceEndpointRepository sourceEndpointRepository;
  private final TagRepository tagRepository;
  private final AdvisorRepository advisorRepository;
  private final ResearchAreaRepository researchAreaRepository;
  private final EntityLookupService entityLookupService;

  public ThesisManagementService(
      ThesisProposalRepository thesisRepository,
      ChairRepository chairRepository,
      SourceEndpointRepository sourceEndpointRepository,
      TagRepository tagRepository,
      AdvisorRepository advisorRepository,
      ResearchAreaRepository researchAreaRepository,
      EntityLookupService entityLookupService) {
    this.thesisRepository = thesisRepository;
    this.chairRepository = chairRepository;
    this.sourceEndpointRepository = sourceEndpointRepository;
    this.tagRepository = tagRepository;
    this.advisorRepository = advisorRepository;
    this.researchAreaRepository = researchAreaRepository;
    this.entityLookupService = entityLookupService;
  }

  @Transactional
  public IngestionResult replaceThesesInDatabase(
      Long sourceEndpointId, SourceEndpointThesesReplacementRequestDto request) {
    log.info(
        "Starting atomic database replacement transaction for sourceEndpointId: {}",
        sourceEndpointId);

    if (sourceEndpointId == null) {
      throw new IllegalArgumentException("sourceEndpointId must not be null");
    }
    if (request == null || request.getTheses() == null) {
      throw new IllegalArgumentException("Replacement request and theses list must not be null");
    }

    SourceEndpoint sourceEndpoint =
        sourceEndpointRepository
            .findById(sourceEndpointId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "SourceEndpoint not found with ID: " + sourceEndpointId));
    Chair chair = sourceEndpoint.getChair();

    if (request.getLastContentHash() != null) {
      sourceEndpoint.setLastContentHash(request.getLastContentHash());
      sourceEndpointRepository.save(sourceEndpoint);
    }

    // 4. Perform side-effect-heavy shared entity synchronization
    entityLookupService.ensureSharedEntitiesExist(request);

    // 5. Atomic deletion and replacement
    long deletedCount = thesisRepository.deleteBySourceEndpointId(sourceEndpointId);
    thesisRepository.flush();

    List<ThesisProposal> entityList = new ArrayList<>();
    // 2. Pre-fetch all shared entities for this transaction to avoid N+1
    Set<String> allTagNames = new HashSet<>();
    Set<String> allAreaNames = new HashSet<>();
    Set<String> allAdvisorEmails = new HashSet<>();
    for (ThesisProposalInputDto dto : request.getTheses()) {
      if (dto.getTags() != null) {
        dto.getTags().stream()
            .map(Utils::normalize)
            .filter(Objects::nonNull)
            .forEach(allTagNames::add);
      }
      String area = normalize(unwrap(dto.getResearchArea()));
      if (area != null) allAreaNames.add(area);

      if (dto.getAdvisors() != null) {
        dto.getAdvisors()
            .forEach(
                a -> {
                  String email = normalize(a.getEmail());
                  if (email != null) allAdvisorEmails.add(email);
                });
      }
    }

    Map<String, Tag> tagMap =
        allTagNames.isEmpty()
            ? Map.of()
            : tagRepository.findAllByNameIn(allTagNames).stream()
                .collect(Collectors.toMap(Tag::getName, t -> t));
    Map<String, ResearchArea> areaMap =
        allAreaNames.isEmpty()
            ? Map.of()
            : researchAreaRepository.findAllByNameIn(allAreaNames).stream()
                .collect(Collectors.toMap(ResearchArea::getName, a -> a));
    Map<String, Advisor> advisorMap =
        allAdvisorEmails.isEmpty()
            ? Map.of()
            : advisorRepository.findAllByEmailIn(allAdvisorEmails).stream()
                .collect(Collectors.toMap(Advisor::getEmail, a -> a));

    for (ThesisProposalInputDto dto : request.getTheses()) {
      ThesisProposal thesis = new ThesisProposal();
      thesis.setChair(chair);
      thesis.setSourceEndpoint(sourceEndpoint);

      String title = normalize(dto.getTitle());
      if (title == null) {
        throw new IllegalArgumentException("Thesis title must not be blank");
      }
      thesis.setTitle(title);

      thesis.setDegreeType(normalize(unwrap(dto.getDegreeType())));
      thesis.setOriginalDescription(unwrap(dto.getOriginalDescription()));
      thesis.setAiOverview(unwrap(dto.getAiOverview()));

      thesis.setSourceUrl(dto.getSourceUrl().toString());

      String normalizedStatus = normalize(dto.getStatus());
      thesis.setStatus(normalizedStatus != null ? normalizedStatus : "OPEN");

      thesis.setLastSeenAt(OffsetDateTime.now());

      if (dto.getTags() != null) {
        Set<Tag> managedTags =
            dto.getTags().stream()
                .map(Utils::normalize)
                .map(tagMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        thesis.setTags(managedTags);
      }

      if (dto.getAdvisors() != null) {
        Set<Advisor> managedAdvisors =
            dto.getAdvisors().stream()
                .map(
                    advDto -> {
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

    return new IngestionResult(entityList, deletedCount);
  }
}
