package com.project95.thesis.thesis.service;

import com.project95.thesis.management.dto.*;
import com.project95.thesis.thesis.config.ClientProperties;
import com.project95.thesis.thesis.domain.ThesisProposal;
import com.project95.thesis.thesis.repository.ThesisProposalRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static com.project95.thesis.thesis.utils.Utils.unwrap;

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

        Pageable pageable = PageRequest.of(
                request.getPage() != null ? request.getPage() : 0,
                request.getSize() != null ? request.getSize() : 20
        );

        List<Long> vectorMatchIds = null;

        if (request.getNaturalLanguageQuery() != null && !request.getNaturalLanguageQuery().isBlank()) {
            vectorMatchIds = callVectorSearch(request);
            if (vectorMatchIds == null) {
                log.warn("Vector search failed. Falling back to relational search only.");
            } else if (vectorMatchIds.isEmpty()) {
                // Vector search succeeded but found nothing - strict match policy
                return emptyResponse(pageable);
            }
        }

        Specification<ThesisProposal> spec = buildSpecification(request.getFilters(), vectorMatchIds);
        
        // If we have vector match IDs, we must ensure ranking is preserved
        if (vectorMatchIds != null && !vectorMatchIds.isEmpty()) {
            // Fetch ALL matching candidates from DB (capped by vector service limit, e.g. 200)
            List<ThesisProposal> allCandidates = thesisRepository.findAll(spec);
            
            // Build rank map for O(1) lookup during sort
            final Map<Long, Integer> rankMap = new HashMap<>();
            for (int i = 0; i < vectorMatchIds.size(); i++) {
                rankMap.put(vectorMatchIds.get(i), i);
            }

            // Re-order based on vector search relevance
            allCandidates.sort(Comparator.comparingInt(t -> rankMap.getOrDefault(t.getId(), Integer.MAX_VALUE)));

            // Manual pagination for the ranked list
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), allCandidates.size());
            
            List<ThesisProposal> pagedList = (start < allCandidates.size()) 
                ? allCandidates.subList(start, end) 
                : Collections.emptyList();

            return mapToResponseDto(new PageImpl<>(pagedList, pageable, allCandidates.size()));
        }

        // Standard relational pagination
        Page<ThesisProposal> page = thesisRepository.findAll(spec, pageable);
        return mapToResponseDto(page);
    }

    private SearchThesesResponseDto mapToResponseDto(Page<ThesisProposal> page) {
        SearchThesesResponseDto response = new SearchThesesResponseDto();
        response.setItems(page.getContent().stream().map(this::mapToSearchResultDto).collect(Collectors.toList()));
        response.setTotalElements((int) page.getTotalElements());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        return response;
    }

    /**
     * @return List of IDs on success, empty list for zero matches, or null on service failure.
     */
    private List<Long> callVectorSearch(SearchThesesRequestDto request) {
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
            VectorSearchResponseDto vectorResponse = restClient.post()
                    .uri("/internal/v1/vector-search-service/search")
                    .body(vectorRequest)
                    .retrieve()
                    .body(VectorSearchResponseDto.class);

            if (vectorResponse != null && vectorResponse.getResults() != null) {
                return vectorResponse.getResults().stream()
                        .map(VectorSearchResultDto::getThesisId)
                        .collect(Collectors.toList());
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to call vector search service", e);
            return null; // Signal failure to allow fallback
        }
    }

    private Specification<ThesisProposal> buildSpecification(ThesisSearchFiltersDto filters, List<Long> includeIds) {
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

    private ThesisSearchResultDto mapToSearchResultDto(ThesisProposal entity) {
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

        if (!entity.getAdvisors().isEmpty()) {
            dto.setAdvisors(
                    entity.getAdvisors().stream()
                            .map(
                                    a -> {
                                        com.project95.thesis.management.dto.AdvisorDto advDto =
                                                new com.project95.thesis.management.dto.AdvisorDto();
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
