package com.project95.thesis.thesis.repository;

import com.project95.thesis.thesis.domain.ThesisProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThesisProposalRepository extends JpaRepository<ThesisProposal, Long> {

    void deleteByChairId(Long chairId);

    List<ThesisProposal> findByStatus(String status);
}
