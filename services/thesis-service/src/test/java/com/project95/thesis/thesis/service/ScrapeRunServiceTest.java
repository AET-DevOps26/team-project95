package com.project95.thesis.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.project95.thesis.management.dto.ScrapeRunLogRequestDto;
import com.project95.thesis.management.dto.ScrapeRunLogResponseDto;
import com.project95.thesis.thesis.domain.ScrapeRun;
import com.project95.thesis.thesis.domain.SourceEndpoint;
import com.project95.thesis.thesis.repository.ScrapeRunRepository;
import com.project95.thesis.thesis.repository.SourceEndpointRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScrapeRunServiceTest {

  @Mock private ScrapeRunRepository scrapeRunRepository;
  @Mock private SourceEndpointRepository sourceEndpointRepository;

  @InjectMocks private ScrapeRunService service;

  @Test
  void logScrapeRun_Success() {
    // Arrange
    ScrapeRunLogRequestDto request = new ScrapeRunLogRequestDto();
    request.setSourceEndpointId(10L);
    request.setStartedAt(OffsetDateTime.now());
    request.setFinishedAt(OffsetDateTime.now());
    request.setStatus(ScrapeRunLogRequestDto.StatusEnum.SUCCESS);

    SourceEndpoint endpoint = new SourceEndpoint();
    endpoint.setId(10L);
    when(sourceEndpointRepository.findById(10L)).thenReturn(Optional.of(endpoint));

    ScrapeRun savedRun = new ScrapeRun();
    savedRun.setId(42L);
    savedRun.setStatus("SUCCESS");
    when(scrapeRunRepository.save(any())).thenReturn(savedRun);

    // Act
    ScrapeRunLogResponseDto response = service.logScrapeRun(request);

    // Assert
    assertThat(response.getId()).isEqualTo(42L);
    assertThat(response.getStatus()).isEqualTo("SUCCESS");
    verify(scrapeRunRepository).save(any());
    verify(sourceEndpointRepository).save(endpoint);
  }

  @Test
  void logScrapeRun_ThrowsIfStartedAtMissing() {
    ScrapeRunLogRequestDto request = new ScrapeRunLogRequestDto();
    request.setSourceEndpointId(10L);
    request.setStatus(ScrapeRunLogRequestDto.StatusEnum.SUCCESS);

    assertThrows(IllegalArgumentException.class, () -> service.logScrapeRun(request));
  }

  @Test
  void logScrapeRun_ThrowsIfStatusMissing() {
    ScrapeRunLogRequestDto request = new ScrapeRunLogRequestDto();
    request.setSourceEndpointId(10L);
    request.setStartedAt(OffsetDateTime.now());

    assertThrows(IllegalArgumentException.class, () -> service.logScrapeRun(request));
  }
}
