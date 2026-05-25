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

  public ThesisManagementService(
          ThesisProposalRepository thesisRepository,
          ChairRepository chairRepository,
          TagRepository tagRepository,
          AdvisorRepository advisorRepository,
          ResearchAreaRepository researchAreaRepository) {
      this.thesisRepository = thesisRepository;
      this.chairRepository = chairRepository;
      this.tagRepository = tagRepository;
      this.advisorRepository = advisorRepository;
      this.researchAreaRepository = researchAreaRepository;
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

    List<ThesisProposal> entityList = new ArrayList<>();
    for (ThesisProposalInput dto : request.getTheses()) {
      ThesisProposal thesis = new ThesisProposal();
      thesis.setChair(chair);
      thesis.setTitle(dto.getTitle());
      
      thesis.setDegreeType(unwrap(dto.getDegreeType()));
      thesis.setOriginalDescription(unwrap(dto.getOriginalDescription()));
      thesis.setAiOverview(unwrap(dto.getAiOverview()));
      
      thesis.setSourceUrl(dto.getSourceUrl() != null ? dto.getSourceUrl().toString() : null);
      thesis.setStatus(dto.getStatus() != null ? dto.getStatus() : "OPEN");
      thesis.setLastSeenAt(OffsetDateTime.now());

      if (dto.getTags() != null) {
        Set<Tag> managedTags = dto.getTags().stream()
          .map(tagName -> tagRepository.findByName(tagName)
            .orElseGet(() -> tagRepository.save(new Tag(tagName))))
            .collect(Collectors.toSet());
        thesis.setTags(managedTags);
      }

      if (dto.getAdvisors() != null) {
        Set<Advisor> managedAdvisors = dto.getAdvisors().stream()
          .map(advDto -> {
            String emailStr = unwrap(advDto.getEmail());
            String nameStr = advDto.getName();
            String profileUrlStr = advDto.getProfileUrl() != null ? advDto.getProfileUrl().toString() : null;

            if (emailStr != null) {
              return advisorRepository.findByEmail(emailStr)
               .orElseGet(() -> advisorRepository.save(new Advisor(nameStr, emailStr, profileUrlStr)));
              }
            return advisorRepository.save(new Advisor(nameStr, null, profileUrlStr));
          })
          .collect(Collectors.toSet());
        thesis.setAdvisors(managedAdvisors);
      }

      String researchAreaStr = unwrap(dto.getResearchArea());
      if (researchAreaStr != null) {
        ResearchArea managedArea = researchAreaRepository.findByName(researchAreaStr)
          .orElseGet(() -> researchAreaRepository.save(new ResearchArea(researchAreaStr)));
        thesis.setResearchAreas(new HashSet<>(Collections.singletonList(managedArea)));
      }

      entityList.add(thesis);
    }

    return thesisRepository.saveAll(entityList);
  }

  private <T> T unwrap(JsonNullable<T> nullable) {
    return (nullable != null && nullable.isPresent()) ? nullable.get() : null;
  }
}
