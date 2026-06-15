package com.project95.thesis.thesis.sourceconfig;

import static org.assertj.core.api.Assertions.assertThat;

import com.project95.thesis.thesis.domain.Chair;
import com.project95.thesis.thesis.domain.SourceEndpoint;
import com.project95.thesis.thesis.repository.ChairRepository;
import com.project95.thesis.thesis.repository.SourceEndpointRepository;
import com.project95.thesis.thesis.sourceconfig.SourceRegistry.ChairEntry;
import com.project95.thesis.thesis.sourceconfig.SourceRegistry.EndpointEntry;
import com.project95.thesis.thesis.sourceconfig.SourceRegistry.RegistryResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SourceRegistryIntegrationTest {
  @Autowired private SourceRegistryService registryService;
  @Autowired private ChairRepository chairRepository;
  @Autowired private SourceEndpointRepository sourceEndpointRepository;

  @BeforeEach
  void setUp() {
    sourceEndpointRepository.deleteAll();
    chairRepository.deleteAll();
  }

  @Test
  void applyRegistry_InsertsAndIsIdempotent() {
    List<ChairEntry> registry = List.of(chair("chair-a", "endpoint-a", "ACTIVE"));

    RegistryResult firstResult = registryService.applyRegistry(registry);
    RegistryResult secondResult = registryService.applyRegistry(registry);

    assertThat(firstResult.chairsInserted()).isEqualTo(1);
    assertThat(firstResult.endpointsInserted()).isEqualTo(1);
    assertThat(secondResult.chairsInserted()).isZero();
    assertThat(secondResult.chairsUpdated()).isZero();
    assertThat(secondResult.endpointsInserted()).isZero();
    assertThat(secondResult.endpointsUpdated()).isZero();
    assertThat(chairRepository.findAll()).hasSize(1);
    assertThat(sourceEndpointRepository.findAll()).hasSize(1);
  }

  @Test
  void applyRegistry_UpdatesExistingRegistryManagedRows() {
    registryService.applyRegistry(List.of(chair("chair-a", "endpoint-a", "ACTIVE")));

    ChairEntry updatedChair =
        new ChairEntry(
            "chair-a",
            "Updated Chair",
            "https://example.com/updated-chair/",
            List.of(new EndpointEntry("endpoint-a", "https://example.com/updated-endpoint/", "RETIRED")));

    RegistryResult result = registryService.applyRegistry(List.of(updatedChair));

    Chair chair = chairRepository.findByRegistryKey("chair-a").orElseThrow();
    SourceEndpoint endpoint = sourceEndpointRepository.findByRegistryKey("endpoint-a").orElseThrow();
    assertThat(result.chairsUpdated()).isEqualTo(1);
    assertThat(result.endpointsUpdated()).isEqualTo(1);
    assertThat(chair.getName()).isEqualTo("Updated Chair");
    assertThat(chair.getWebsiteUrl()).isEqualTo("https://example.com/updated-chair/");
    assertThat(endpoint.getUrl()).isEqualTo("https://example.com/updated-endpoint/");
    assertThat(endpoint.getStatus()).isEqualTo("RETIRED");
  }

  @Test
  void applyRegistry_RetiresRemovedRegistryManagedEndpointsOnly() {
    registryService.applyRegistry(
        List.of(chair("chair-a", "endpoint-a", "ACTIVE"), chair("chair-b", "endpoint-b", "ACTIVE")));

    Chair unmanagedChair = chairRepository.save(new Chair("Manual Chair", "https://manual.example.com/"));
    SourceEndpoint unmanagedEndpoint = new SourceEndpoint();
    unmanagedEndpoint.setChair(unmanagedChair);
    unmanagedEndpoint.setUrl("https://manual.example.com/theses/");
    unmanagedEndpoint.setStatus("ACTIVE");
    sourceEndpointRepository.save(unmanagedEndpoint);

    RegistryResult result = registryService.applyRegistry(List.of(chair("chair-a", "endpoint-a", "ACTIVE")));

    assertThat(result.endpointsRetired()).isEqualTo(1);
    assertThat(sourceEndpointRepository.findByRegistryKey("endpoint-a").orElseThrow().getStatus())
        .isEqualTo("ACTIVE");
    assertThat(sourceEndpointRepository.findByRegistryKey("endpoint-b").orElseThrow().getStatus())
        .isEqualTo("RETIRED");
    assertThat(sourceEndpointRepository.findById(unmanagedEndpoint.getId()).orElseThrow().getStatus())
        .isEqualTo("ACTIVE");
  }

  @Test
  void applyRegistry_AttachesRegistryKeyToExistingUnmanagedRows() {
    Chair chair = chairRepository.save(new Chair("Chair chair-a", "https://example.com/chair-a/"));
    SourceEndpoint endpoint = new SourceEndpoint();
    endpoint.setChair(chair);
    endpoint.setUrl("https://example.com/endpoint-a/theses/");
    endpoint.setStatus("ACTIVE");
    sourceEndpointRepository.save(endpoint);

    RegistryResult result = registryService.applyRegistry(List.of(chair("chair-a", "endpoint-a", "ACTIVE")));

    assertThat(result.chairsInserted()).isZero();
    assertThat(result.endpointsInserted()).isZero();
    assertThat(result.chairsUpdated()).isEqualTo(1);
    assertThat(result.endpointsUpdated()).isEqualTo(1);
    assertThat(chairRepository.findAll()).hasSize(1);
    assertThat(sourceEndpointRepository.findAll()).hasSize(1);
    assertThat(chairRepository.findByRegistryKey("chair-a")).isPresent();
    assertThat(sourceEndpointRepository.findByRegistryKey("endpoint-a")).isPresent();
  }

  private static ChairEntry chair(String chairKey, String endpointKey, String status) {
    return new ChairEntry(
        chairKey,
        "Chair " + chairKey,
        "https://example.com/" + chairKey + "/",
        List.of(new EndpointEntry(endpointKey, "https://example.com/" + endpointKey + "/theses/", status)));
  }
}
