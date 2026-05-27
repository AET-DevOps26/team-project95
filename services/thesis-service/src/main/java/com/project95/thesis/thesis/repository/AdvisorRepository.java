package com.project95.thesis.thesis.repository;

import com.project95.thesis.thesis.domain.Advisor;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdvisorRepository extends JpaRepository<Advisor, Long> {
  Optional<Advisor> findByEmail(String email);

  List<Advisor> findAllByEmailIn(Collection<String> emails);
}
