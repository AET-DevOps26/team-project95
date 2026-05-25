package com.project95.thesis.thesis.repository;

import com.project95.thesis.thesis.domain.ResearchArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ResearchAreaRepository extends JpaRepository<ResearchArea, Long> {
    Optional<ResearchArea> findByName(String name);
}
