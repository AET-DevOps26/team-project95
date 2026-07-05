package com.project95.thesis.scraping.controller;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
class ScrapeControllerIntegrationTest {

  private static final String OPENAPI_SPEC = "../../api/openapi-v1.yml";

  @Autowired private MockMvc mockMvc;

  private static MockRestServiceServer thesisServer;
  private static MockRestServiceServer genAiServer;
  private static MockRestServiceServer scrapingServer;

  @TestConfiguration
  static class TestConfig {
    @Bean
    @Primary
    public RestClient thesisServiceClient(RestClient.Builder builder) {
      thesisServer = MockRestServiceServer.bindTo(builder).build();
      return builder.baseUrl("http://localhost:8080").build();
    }

    @Bean
    public RestClient genAiServiceClient(RestClient.Builder builder) {
      genAiServer = MockRestServiceServer.bindTo(builder).build();
      return builder.baseUrl("http://localhost:8000").build();
    }

    @Bean
    public RestClient scrapingClient(RestClient.Builder builder) {
      scrapingServer = MockRestServiceServer.bindTo(builder).build();
      return builder.build();
    }
  }

  @BeforeEach
  void setUp() {
    thesisServer.reset();
    genAiServer.reset();
    scrapingServer.reset();
  }

  @Test
  void triggerScrape_StartsAsynchronouslyAndSucceeds() throws Exception {
    // 1. Mock Thesis Service fetching endpoints
    thesisServer
        .expect(requestTo("http://localhost:8080/internal/v1/thesis-service/source-endpoints"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{\"endpoints\":[{\"id\":1,\"chairId\":10,\"chairName\":\"AI"
                    + " Chair\",\"url\":\"http://chair.example.com/theses\",\"status\":\"ACTIVE\"}]}",
                MediaType.APPLICATION_JSON));

    // 2. Mock Scraping Client fetching raw HTML
    scrapingServer
        .expect(requestTo("http://chair.example.com/theses"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("<html><h1>AI Thesis</h1></html>", MediaType.TEXT_HTML));

    // 2b. Mock Thesis Service detecting changes
    thesisServer
        .expect(
            requestTo("http://localhost:8080/thesis-internal/v1/source-endpoints/1/detect-changes"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                "{\"changed\":true,\"sanitizedHtml\":\"<html><h1>AI"
                    + " Thesis</h1></html>\",\"contentHash\":\"some-hash\"}",
                MediaType.APPLICATION_JSON));

    // 3. Mock GenAI Service extracting theses
    genAiServer
        .expect(requestTo("http://localhost:8000/internal/v1/genai-service/extract-theses"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                "{\"theses\":[{\"title\":\"AI"
                    + " Thesis\",\"sourceUrl\":\"http://chair.example.com/theses\",\"degreeType\":\"MASTER\",\"status\":\"OPEN\"}]}",
                MediaType.APPLICATION_JSON));

    // 4. Mock Thesis Service replacing proposals
    thesisServer
        .expect(
            requestTo("http://localhost:8080/internal/v1/thesis-service/source-endpoints/1/theses"))
        .andExpect(method(HttpMethod.PUT))
        .andRespond(withSuccess());

    // 5. Mock Thesis Service logging SUCCESS
    thesisServer
        .expect(requestTo("http://localhost:8080/internal/v1/thesis-service/scrape-runs"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess());

    // Act: trigger scraping
    mockMvc
        .perform(post("/internal/v1/scraping-service/scrape"))
        .andExpect(status().isAccepted())
        .andExpect(openApi().isValid(OPENAPI_SPEC))
        .andExpect(jsonPath("$.started").value(true))
        .andExpect(jsonPath("$.message").value("Scrape run started."));

    // Wait for the asynchronous background task to complete execution
    Thread.sleep(1200);

    // Assert: verify that all mock servers received their respective calls
    thesisServer.verify();
    genAiServer.verify();
    scrapingServer.verify();
  }

  @Test
  void triggerScrape_StartsAsynchronouslyAndLogsFailureOnScrapeException() throws Exception {
    // 1. Mock Thesis Service fetching endpoints
    thesisServer
        .expect(requestTo("http://localhost:8080/internal/v1/thesis-service/source-endpoints"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{\"endpoints\":[{\"id\":1,\"chairId\":10,\"chairName\":\"AI"
                    + " Chair\",\"url\":\"http://chair.example.com/theses\",\"status\":\"ACTIVE\"}]}",
                MediaType.APPLICATION_JSON));

    // 2. Mock Scraping Client failing (e.g., website down)
    scrapingServer
        .expect(requestTo("http://chair.example.com/theses"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    // 3. Mock Thesis Service logging FAILURE (skip replacement PUT call)
    thesisServer
        .expect(requestTo("http://localhost:8080/internal/v1/thesis-service/scrape-runs"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess());

    // Act: trigger scraping
    mockMvc
        .perform(post("/internal/v1/scraping-service/scrape"))
        .andExpect(status().isAccepted())
        .andExpect(openApi().isValid(OPENAPI_SPEC));

    // Wait for the asynchronous background task to complete execution
    Thread.sleep(1200);

    // Assert: verify that the failure log was routed and no GenAI or ingestion PUT was fired
    thesisServer.verify();
    scrapingServer.verify();
    genAiServer.verify(); // No expected calls, should pass verification since no expectations were
    // registered
  }
}
