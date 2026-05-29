package com.project95.thesis.thesis.controller;

import com.project95.thesis.management.dto.AvailableFiltersResponseDto;
import com.project95.thesis.management.dto.ChairDto;
import com.project95.thesis.thesis.domain.Chair;
import com.project95.thesis.thesis.domain.ResearchArea;
import com.project95.thesis.thesis.domain.Tag;
import com.project95.thesis.thesis.repository.ChairRepository;
import com.project95.thesis.thesis.repository.ResearchAreaRepository;
import com.project95.thesis.thesis.repository.TagRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class FilterController {

  private final ChairRepository chairRepository;
  private final TagRepository tagRepository;
  private final ResearchAreaRepository researchAreaRepository;

  public FilterController(
      ChairRepository chairRepository,
      TagRepository tagRepository,
      ResearchAreaRepository researchAreaRepository) {
    this.chairRepository = chairRepository;
    this.tagRepository = tagRepository;
    this.researchAreaRepository = researchAreaRepository;
  }

  @GetMapping("/chairs")
  public ResponseEntity<List<ChairDto>> listChairs() {
    return ResponseEntity.ok(
        chairRepository.findAll().stream().map(this::mapToChairDto).collect(Collectors.toList()));
  }

  @GetMapping("/filters")
  public ResponseEntity<AvailableFiltersResponseDto> getAvailableFilters() {
    AvailableFiltersResponseDto response = new AvailableFiltersResponseDto();

    response.setChairs(
        chairRepository.findAll().stream().map(this::mapToChairDto).collect(Collectors.toList()));

    response.setTags(
        tagRepository.findAll().stream().map(Tag::getName).collect(Collectors.toList()));

    response.setResearchAreas(
        researchAreaRepository.findAll().stream()
            .map(ResearchArea::getName)
            .collect(Collectors.toList()));

    response.setDegreeTypes(List.of("BACHELOR", "MASTER", "INTERNSHIP", "IDP"));

    return ResponseEntity.ok(response);
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
