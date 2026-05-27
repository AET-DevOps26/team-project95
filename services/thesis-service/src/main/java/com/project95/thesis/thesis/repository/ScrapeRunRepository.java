package com.project95.thesis.thesis.repository;

import com.project95.thesis.thesis.domain.ScrapeRun;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScrapeRunRepository extends JpaRepository<ScrapeRun, Long> {
  List<ScrapeRun> findBySourceEndpointId(Long sourceEndpointId);
}
