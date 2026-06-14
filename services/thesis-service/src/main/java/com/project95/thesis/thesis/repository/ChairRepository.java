package com.project95.thesis.thesis.repository;

import com.project95.thesis.thesis.domain.Chair;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChairRepository extends JpaRepository<Chair, Long> {
  // Allows looking up a chair by its unique name if needed
  Optional<Chair> findByName(String name);

  Optional<Chair> findByRegistryKey(String registryKey);

  List<Chair> findByRegistryKeyIn(Collection<String> registryKeys);
}
