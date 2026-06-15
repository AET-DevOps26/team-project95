package com.project95.thesis.thesis.repository;

import com.project95.thesis.thesis.domain.SourceEndpoint;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SourceEndpointRepository extends JpaRepository<SourceEndpoint, Long> {
  List<SourceEndpoint> findByChairId(Long chairId);

  Optional<SourceEndpoint> findByRegistryKey(String registryKey);

  List<SourceEndpoint> findByRegistryKeyIn(Collection<String> registryKeys);

  List<SourceEndpoint> findByRegistryKeyIsNotNull();

  @EntityGraph(attributePaths = "chair")
  @Query("SELECT s FROM SourceEndpoint s")
  List<SourceEndpoint> findAllWithChairEagerly();

  @EntityGraph(attributePaths = "chair")
  @Query("SELECT s FROM SourceEndpoint s WHERE s.status = 'ACTIVE'")
  List<SourceEndpoint> findActiveWithChairEagerly();
}
