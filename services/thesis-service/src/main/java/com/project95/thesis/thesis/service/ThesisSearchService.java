package com.project95.thesis.thesis.service;

import static com.project95.thesis.thesis.utils.Utils.unwrap;

import com.project95.thesis.management.dto.*;
import com.project95.thesis.thesis.domain.ThesisProposal;
import com.project95.thesis.thesis.repository.ThesisProposalRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ThesisSearchService {

  private static final Logger log = LoggerFactory.getLogger(ThesisSearchService.class);

  private final ThesisProposalRepository thesisRepository;
  private final RestClient restClient;

  public ThesisSearchService(ThesisProposalRepository thesisRepository, RestClient restClient) {
    this.thesisRepository = thesisRepository;
    this.restClient = restClient;
  }

  public SearchThesesResponseDto searchTheses(SearchThesesRequestDto request) {
    log.info("Searching theses with request: {}", request);

    Pageable pageable =
        PageRequest.of(
            request.getPage() != null ? request.getPage() : 0,
            request.getSize() != null ? request.getSize() : 20);

    Map<Long, Double> vectorMatchScores = null;

    if (request.getNaturalLanguageQuery() != null && !request.getNaturalLanguageQuery().isBlank()) {
      vectorMatchScores = callVectorSearch(request);
      if (vectorMatchScores == null) {
        log.warn("Vector search failed. Falling back to relational search only.");
      } else if (vectorMatchScores.isEmpty()) {
        // Vector search succeeded but found nothing - strict match policy
        return emptyResponse(pageable);
      }
    }

    List<Long> includeIds =
        vectorMatchScores != null ? new ArrayList<>(vectorMatchScores.keySet()) : null;
    Specification<ThesisProposal> spec = buildSpecification(request.getFilters(), includeIds);

    // If we have vector match IDs, we must ensure ranking is preserved
    if (vectorMatchScores != null && !vectorMatchScores.isEmpty()) {
      // Fetch ALL matching candidates from DB (capped by vector service limit, e.g. 200)
      List<ThesisProposal> allCandidates = thesisRepository.findAll(spec);

      // Re-order based on vector search relevance
      final Map<Long, Double> finalScores = vectorMatchScores;
      allCandidates.sort(
          Comparator.comparingDouble(t -> -finalScores.getOrDefault(t.getId(), 0.0)));

      // Manual pagination for the ranked list
      int start = (int) pageable.getOffset();
      int end = Math.min((start + pageable.getPageSize()), allCandidates.size());

      List<ThesisProposal> pagedList =
          (start < allCandidates.size())
              ? allCandidates.subList(start, end)
              : Collections.emptyList();

      return mapToResponseDto(
          new PageImpl<>(pagedList, pageable, allCandidates.size()), vectorMatchScores);
    }

    // Standard relational pagination
    Page<ThesisProposal> page = thesisRepository.findAll(spec, pageable);
    return mapToResponseDto(page, null);
  }

  public List<ThesisSearchResultDto> listAllTheses() {
    log.info("Listing all theses");
    return thesisRepository.findAll().stream()
        .map(entity -> mapToSearchResultDto(entity, null))
        .collect(Collectors.toList());
  }

  private SearchThesesResponseDto mapToResponseDto(
      Page<ThesisProposal> page, Map<Long, Double> scores) {
    SearchThesesResponseDto response = new SearchThesesResponseDto();
    response.setItems(
        page.getContent().stream()
            .map(
                entity ->
                    mapToSearchResultDto(entity, scores != null ? scores.get(entity.getId()) : null))
            .collect(Collectors.toList()));
    response.setTotalElements((int) page.getTotalElements());
    response.setPage(page.getNumber());
    response.setSize(page.getSize());
    return response;
  }

  /** @return Map of ID -> Score on success, empty map for zero matches, or null on service failure. */
  private Map<Long, Double> callVectorSearch(SearchThesesRequestDto request) {
    VectorSearchRequestDto vectorRequest = new VectorSearchRequestDto();
    vectorRequest.setQuery(request.getNaturalLanguageQuery());

    if (request.getFilters() != null) {
      VectorSearchFiltersDto vectorFilters = new VectorSearchFiltersDto();
      vectorFilters.setChairIds(request.getFilters().getChairIds());
      vectorFilters.setDegreeTypes(request.getFilters().getDegreeTypes());
      vectorRequest.setFilters(vectorFilters);
    }

    vectorRequest.setLimit(200);

    try {
      VectorSearchResponseDto vectorResponse =
          restClient
              .post()
              .uri("/internal/v1/vector-search-service/search")
              .body(vectorRequest)
              .retrieve()
              .body(VectorSearchResponseDto.class);

      if (vectorResponse != null && vectorResponse.getResults() != null) {
        Map<Long, Double> scores = new LinkedHashMap<>();
        for (VectorSearchResultDto res : vectorResponse.getResults()) {
          if (res.getThesisId() != null) {
            scores.put(res.getThesisId(), (double) res.getScore());
          }
        }
        return scores;
      }
      return null;
    } catch (Exception e) {
      log.error("Failed to call vector search service", e);
      return null; // Signal failure to allow fallback
    }
  }

  private Specification<ThesisProposal> buildSpecification(
      ThesisSearchFiltersDto filters, List<Long> includeIds) {
    return (root, query, cb) -> {
      // Ensure distinct results to prevent duplicates from many-to-many joins
      query.distinct(true);

      List<Predicate> predicates = new ArrayList<>();

      if (includeIds != null && !includeIds.isEmpty()) {
        predicates.add(root.get("id").in(includeIds));
      }

      if (filters != null) {
        if (filters.getChairIds() != null && !filters.getChairIds().isEmpty()) {
          predicates.add(root.get("chair").get("id").in(filters.getChairIds()));
        }
        if (filters.getDegreeTypes() != null && !filters.getDegreeTypes().isEmpty()) {
          predicates.add(root.get("degreeType").in(filters.getDegreeTypes()));
        }
        if (filters.getStatus() != null) {
          predicates.add(cb.equal(root.get("status"), unwrap(filters.getStatus())));
        }
        if (filters.getResearchAreas() != null && !filters.getResearchAreas().isEmpty()) {
          Join<Object, Object> areaJoin = root.join("researchAreas");
          predicates.add(areaJoin.get("name").in(filters.getResearchAreas()));
        }
        if (filters.getTags() != null && !filters.getTags().isEmpty()) {
          Join<Object, Object> tagJoin = root.join("tags");
          predicates.add(tagJoin.get("name").in(filters.getTags()));
        }
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private ThesisSearchResultDto mapToSearchResultDto(ThesisProposal entity, Double score) {
    ThesisSearchResultDto dto = new ThesisSearchResultDto();
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

    if (score != null) {
      dto.setSemanticScore(score.floatValue());
    }

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

  private SearchThesesResponseDto emptyResponse(Pageable pageable) {
    SearchThesesResponseDto response = new SearchThesesResponseDto();
    response.setItems(new ArrayList<>());
    response.setTotalElements(0);
    response.setPage(pageable.getPageNumber());
    response.setSize(pageable.getPageSize());
    return response;
  }
}