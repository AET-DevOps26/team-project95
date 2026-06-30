package com.project95.thesis.thesis.controller;

import com.project95.thesis.management.api.FrontendApiApi;
import com.project95.thesis.management.dto.*;
import com.project95.thesis.thesis.domain.Chair;
import com.project95.thesis.thesis.domain.Tag;
import com.project95.thesis.thesis.domain.ThesisProposal;
import com.project95.thesis.thesis.repository.ChairRepository;
import com.project95.thesis.thesis.repository.ResearchAreaRepository;
import com.project95.thesis.thesis.repository.TagRepository;
import com.project95.thesis.thesis.repository.ThesisProposalRepository;
import com.project95.thesis.thesis.service.ThesisSearchService;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicThesisController implements FrontendApiApi {

  private final ThesisSearchService thesisSearchService;
  private final ThesisProposalRepository thesisRepository;
  private final ChairRepository chairRepository;
  private final TagRepository tagRepository;
  private final ResearchAreaRepository researchAreaRepository;

  public PublicThesisController(
      ThesisSearchService thesisSearchService,
      ThesisProposalRepository thesisRepository,
      ChairRepository chairRepository,
      TagRepository tagRepository,
      ResearchAreaRepository researchAreaRepository) {
    this.thesisSearchService = thesisSearchService;
    this.thesisRepository = thesisRepository;
    this.chairRepository = chairRepository;
    this.tagRepository = tagRepository;
    this.researchAreaRepository = researchAreaRepository;
  }

  @Override
  public ResponseEntity<List<ThesisSearchResultDto>> listTheses() {
    return ResponseEntity.ok(thesisSearchService.listAllTheses());
  }

  @Override
  public ResponseEntity<SearchThesesResponseDto> searchTheses(
      SearchThesesRequestDto searchThesesRequestDto) {
    return ResponseEntity.ok(thesisSearchService.searchTheses(searchThesesRequestDto));
  }

  @Override
  public ResponseEntity<ThesisProposalDto> getThesisById(Long thesisId) {
    return thesisRepository
        .findById(thesisId)
        .map(this::mapToDto)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<List<ChairDto>> listChairs() {
    return ResponseEntity.ok(
        chairRepository.findAll().stream().map(this::mapToChairDto).collect(Collectors.toList()));
  }

  @Override
  public ResponseEntity<AvailableFiltersResponseDto> getAvailableFilters() {
    AvailableFiltersResponseDto response = new AvailableFiltersResponseDto();

    response.setChairs(
        chairRepository.findAll().stream().map(this::mapToChairDto).collect(Collectors.toList()));

    response.setTags(
        tagRepository.findAll().stream().map(Tag::getName).collect(Collectors.toList()));

    response.setResearchAreas(researchAreaRepository.findDistinctNamesLinkedToTheses());

    response.setDegreeTypes(List.of("BACHELOR", "MASTER", "INTERNSHIP", "IDP"));

    return ResponseEntity.ok(response);
  }

  private ThesisProposalDto mapToDto(ThesisProposal entity) {
    ThesisProposalDto dto = new ThesisProposalDto();
    dto.setId(entity.getId());
    dto.setTitle(entity.getTitle());
    dto.setChairId(entity.getChair().getId());
    dto.setChairName(entity.getChair().getName());
    dto.setDegreeType(entity.getDegreeType());
    dto.setOriginalDescription(entity.getOriginalDescription());
    dto.setAiOverview(entity.getAiOverview());
    dto.setSourceUrl(URI.create(entity.getSourceUrl()));
    dto.setStatus(entity.getStatus());
    dto.setLastSeenAt(entity.getLastSeenAt());

    if (!entity.getAdvisors().isEmpty()) {
      dto.setAdvisors(
          entity.getAdvisors().stream()
              .map(
                  a -> {
                    com.project95.thesis.management.dto.AdvisorDto advDto =
                        new com.project95.thesis.management.dto.AdvisorDto();
                    advDto.setId(a.getId());
                    advDto.setName(a.getName());
                    advDto.setEmail(a.getEmail());
                    if (a.getProfileUrl() != null) {
                      advDto.setProfileUrl(URI.create(a.getProfileUrl()));
                    }
                    return advDto;
                  })
              .collect(Collectors.toList()));
    }

    if (!entity.getTags().isEmpty()) {
      dto.setTags(entity.getTags().stream().map(Tag::getName).collect(Collectors.toList()));
    }

    if (!entity.getResearchAreas().isEmpty()) {
      dto.setResearchArea(entity.getResearchAreas().iterator().next().getName());
    }

    return dto;
  }

  private ChairDto mapToChairDto(Chair entity) {
    ChairDto dto = new ChairDto();
    dto.setId(entity.getId());
    dto.setName(entity.getName());
    if (entity.getWebsiteUrl() != null) {
      dto.setWebsiteUrl(java.net.URI.create(entity.getWebsiteUrl()));
    }
    return dto;
  }
}
