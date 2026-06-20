package com.project95.thesis.thesis;

import static org.assertj.core.api.Assertions.assertThat;

import com.project95.thesis.thesis.domain.Chair;
import com.project95.thesis.thesis.repository.ChairRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("flyway-test")
class FlywayMigrationIntegrationTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

  @Autowired
  private ChairRepository chairRepository;

  @Test
  void contextLoadsAndFlywayMigrationsSucceed() {
    assertThat(postgres.isRunning()).isTrue();
    
    // Verify that we can interact with the schema created by Flyway
    Chair chair = new Chair("Test Chair", "http://test.com");
    Chair saved = chairRepository.save(chair);
    
    assertThat(saved.getId()).isNotNull();
    assertThat(chairRepository.findById(saved.getId())).isPresent();
  }
}
