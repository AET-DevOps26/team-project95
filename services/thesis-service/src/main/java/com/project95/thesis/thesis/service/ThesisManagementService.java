package com.project95.thesis.thesis.service;

import com.project95.thesis.management.dto.ChairThesesReplacementRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ThesisManagementService {
  private static final Logger log = LoggerFactory.getLogger(ThesisManagementService.class);

  @Transactional
  public int replaceThesesInDatabase(Long chairId, ChairThesesReplacementRequest request) {
    log.info("Starting atomic database replacement transaction for chairId: {}", chairId);
    // TODO
    return request.getTheses() != null ? request.getTheses().size() : 0;
  }
}
