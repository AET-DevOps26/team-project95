package com.project95.thesis.thesis.repository;

import com.project95.thesis.thesis.domain.Chair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ChairRepository extends JpaRepository<Chair, Long> {
    // Allows looking up a chair by its unique name if needed
    Optional<Chair> findByName(String name);
}
