package com.project95.thesis.thesis.controller;

import com.project95.thesis.management.dto.SearchThesesRequestDto;
import com.project95.thesis.management.dto.SearchThesesResponseDto;
import com.project95.thesis.management.dto.ThesisProposalDto;
import com.project95.thesis.management.dto.ThesisSearchResultDto;
import com.project95.thesis.thesis.domain.ThesisProposal;
import com.project95.thesis.thesis.repository.ThesisProposalRepository;
import com.project95.thesis.thesis.service.ThesisSearchService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/theses")
public class PublicThesisController {

  private final ThesisSearchService thesisSearchService;
  private final ThesisProposalRepository thesisRepository;

  public PublicThesisController(
      ThesisSearchService thesisSearchService, ThesisProposalRepository thesisRepository) {
    this.thesisSearchService = thesisSearchService;
    this.thesisRepository = thesisRepository;
  }

  @GetMapping
  public ResponseEntity<List<ThesisSearchResultDto>> listTheses() {
    return ResponseEntity.ok(thesisSearchService.listAllTheses());
  }

  @PostMapping("/search")
  public ResponseEntity<SearchThesesResponseDto> searchTheses(
      @Valid @RequestBody SearchThesesRequestDto request) {
    return ResponseEntity.ok(thesisSearchService.searchTheses(request));
  }

  @GetMapping("/{thesisId}")
  public ResponseEntity<ThesisProposalDto> getThesisById(@PathVariable("thesisId") Long thesisId) {
    return thesisRepository
        .findById(thesisId)
        .map(this::mapToDto)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
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
      dto.setTags(entity.getTags().stream().map(t -> t.getName()).collect(Collectors.toList()));
    }

    if (!entity.getResearchAreas().isEmpty()) {
      dto.setResearchArea(entity.getResearchAreas().iterator().next().getName());
    }

    return dto;
  }
}
