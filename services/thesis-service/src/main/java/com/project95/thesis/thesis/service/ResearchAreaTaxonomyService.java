package com.project95.thesis.thesis.service;

import com.project95.thesis.thesis.repository.ResearchAreaRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class ResearchAreaTaxonomyService {

  private static final String TAXONOMY_RESOURCE = "canonical-research-areas.yml";

  private final ResearchAreaRepository researchAreaRepository;
  private final List<String> canonicalResearchAreas;
  private final Map<String, String> canonicalByCasefold;

  public ResearchAreaTaxonomyService(ResearchAreaRepository researchAreaRepository) {
    this.researchAreaRepository = researchAreaRepository;
    this.canonicalResearchAreas = loadCanonicalResearchAreas();
    this.canonicalByCasefold =
        canonicalResearchAreas.stream()
            .collect(Collectors.toUnmodifiableMap(this::key, Function.identity(), (a, b) -> a));
  }

  public List<String> listKnownResearchAreasForExtraction() {
    Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    names.addAll(canonicalResearchAreas);
    names.addAll(researchAreaRepository.findDistinctNamesLinkedToTheses());
    return new ArrayList<>(names);
  }

  public List<String> listResearchAreasForFilters() {
    return listKnownResearchAreasForExtraction();
  }

  public String canonicalize(String name) {
    if (name == null) {
      return null;
    }
    String trimmed = name.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    return canonicalByCasefold.getOrDefault(key(trimmed), trimmed);
  }

  public boolean isKnownResearchArea(String name) {
    if (name == null) {
      return false;
    }
    return canonicalByCasefold.containsKey(key(name));
  }

  public boolean isAllowedResearchArea(String name) {
    return isKnownResearchArea(name)
        || isExistingResearchArea(name)
        || isAcceptableNewResearchArea(name);
  }

  private boolean isExistingResearchArea(String name) {
    if (name == null || name.isBlank()) {
      return false;
    }
    return researchAreaRepository.findByName(name.trim()).isPresent();
  }

  public static boolean isAcceptableNewResearchArea(String name) {
    if (name == null) {
      return false;
    }

    String trimmed = name.trim();
    if (trimmed.length() < 3 || trimmed.length() > 60) {
      return false;
    }
    if (!trimmed.matches("[\\p{L}0-9][\\p{L}0-9 &/+-]*")) {
      return false;
    }

    String[] words = trimmed.split("\\s+");
    if (words.length > 4) {
      return false;
    }

    return true;
  }

  private List<String> loadCanonicalResearchAreas() {
    ClassPathResource resource = new ClassPathResource(TAXONOMY_RESOURCE);
    try {
      String yaml = resource.getContentAsString(StandardCharsets.UTF_8);
      Set<String> areas = new LinkedHashSet<>();
      boolean inResearchAreas = false;

      for (String line : yaml.split("\\R")) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
          continue;
        }
        if (trimmed.equals("researchAreas:")) {
          inResearchAreas = true;
          continue;
        }
        if (inResearchAreas) {
          if (!trimmed.startsWith("- ")) {
            break;
          }
          String area = trimmed.substring(2).trim();
          if (!area.isEmpty()) {
            areas.add(area);
          }
        }
      }

      if (areas.isEmpty()) {
        throw new IllegalStateException("No research areas configured in " + TAXONOMY_RESOURCE);
      }
      return List.copyOf(areas);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load " + TAXONOMY_RESOURCE, e);
    }
  }

  private String key(String value) {
    return value.trim().toLowerCase(Locale.ROOT);
  }
}
