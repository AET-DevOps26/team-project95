package com.project95.thesis.thesis.repository;

import com.project95.thesis.thesis.domain.ResearchArea;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ResearchAreaRepository extends JpaRepository<ResearchArea, Long> {
  Optional<ResearchArea> findByName(String name);

  List<ResearchArea> findAllByNameIn(Collection<String> names);

  @Query(
      "select distinct researchArea.name from ResearchArea researchArea "
          + "join researchArea.thesisProposals "
          + "order by researchArea.name")
  List<String> findDistinctNamesLinkedToTheses();
}
