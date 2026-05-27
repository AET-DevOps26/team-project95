package com.project95.thesis.thesis.repository;

import com.project95.thesis.thesis.domain.ThesisProposal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThesisProposalRepository extends JpaRepository<ThesisProposal, Long> {

  long deleteByChairId(Long chairId);

  List<ThesisProposal> findByStatus(String status);

  List<ThesisProposal> findAllByAdvisorsId(Long advisorId);
}
