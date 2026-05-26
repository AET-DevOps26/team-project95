# Design Draft: Distributed Scraping Architecture with RabbitMQ

## Context & Motivation
The current scraping architecture uses a centralized `@Scheduled` cron job inside the `scraping-service`. In a scaled-out Kubernetes environment, this leads to:
1.  **Concurrency Issues:** Every instance of the `scraping-service` attempts to run the cron job simultaneously.
2.  **Poor Work Distribution:** A single REST trigger causes one instance to handle the entire workload while others remain idle.
3.  **Tightly Coupled Logic:** The `scraping-service` is responsible for both the "When" (scheduling) and the "How" (scraping).

## Proposed Architecture: The "Competing Consumers" Pattern
We will transition to an event-driven model where the **Thesis Service** acts as the *Orchestrator* and the **Scraping Service** acts as a *Distributed Worker Pool*.

### 1. Orchestration (Thesis Service)
- **Centralized Scheduler:** Move the `@Scheduled` cron job to the `thesis-service`.
- **Task Producer:** Instead of calling a REST endpoint, the scheduler fetches all `SourceEndpoints` and publishes one message per endpoint to a RabbitMQ Exchange.
- **Distributed Locking:** Use `ShedLock` on the `thesis-service` scheduler to ensure only one orchestrator instance publishes tasks.

### 2. Message Broker (RabbitMQ)
- **Queue:** `scraping.tasks.queue`
- **Exchange:** `scraping.exchange` (Direct or Fanout)
- **Durability:** Messages should be persistent to ensure no tasks are lost if the broker restarts.

### 3. Execution (Scraping Service)
- **Task Consumer:** Remove the REST `/scrape` loop and the `@Scheduled` annotation.
- **Parallel Processing:** Implement a `@RabbitListener` that picks up one message at a time.
- **Scaling:** Scaling to *N* replicas will allow *N* endpoints to be scraped in parallel across the cluster.

---

## Implementation Roadmap

### Phase 1: Infrastructure
- Add a `rabbitmq:3-management` service to `docker-compose.yml`.
- Add `spring-boot-starter-amqp` to both services' `pom.xml`.

### Phase 2: Thesis Service (Publisher)
- Configure `RabbitTemplate` and define the Queue/Exchange beans.
- Update `ScrapeTriggerService` to loop through endpoints and call `rabbitTemplate.convertAndSend()`.
- Implement `ShedLock` to prevent duplicate publishing.

### Phase 3: Scraping Service (Consumer)
- Implement a `ScrapeMessageListener` class.
- Refactor `ScrapeCoordinationService` to accept a single `EndpointID` or `DTO` rather than fetching all endpoints.
- Configure `spring.rabbitmq.listener.simple.prefetch` to `1` to ensure fair distribution.

### Phase 4: Feedback Loop (Optional)
- Upon completion, the `scraping-service` can publish a "Task Completed" message to a different queue.
- `thesis-service` listens to this queue to update the `ScrapeRun` status in real-time.

---

## Key Configuration Changes (Draft)

### application.yaml (Shared)
```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: 5672
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASSWORD:guest}
```

### Scraping Task DTO
```java
public record ScrapeTask(
    Long endpointId,
    Long chairId,
    String url
) {}
```

## Status: DRAFT
*This document serves as the technical specification for when the team is ready to implement asynchronous parallel scraping.*
