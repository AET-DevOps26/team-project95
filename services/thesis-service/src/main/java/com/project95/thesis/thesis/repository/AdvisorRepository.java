package com.project95.thesis.thesis.repository;

import com.project95.thesis.thesis.domain.Advisor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AdvisorRepository extends JpaRepository<Advisor, Long> {
    Optional<Advisor> findByEmail(String email);
}
