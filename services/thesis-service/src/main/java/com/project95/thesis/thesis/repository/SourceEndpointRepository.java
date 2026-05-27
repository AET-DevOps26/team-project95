package com.project95.thesis.thesis.repository;

import com.project95.thesis.thesis.domain.SourceEndpoint;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SourceEndpointRepository extends JpaRepository<SourceEndpoint, Long> {
  List<SourceEndpoint> findByChairId(Long chairId);
}
