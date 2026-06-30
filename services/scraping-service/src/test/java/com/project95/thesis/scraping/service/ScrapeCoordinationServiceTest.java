package com.project95.thesis.scraping.service;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.project95.thesis.scraping.config.ClientProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ScrapeCoordinationServiceTest {

  private MockRestServiceServer mockServer;
  private ScrapeCoordinationService service;

  @BeforeEach
  void setUp() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

    ClientProperties properties = new ClientProperties();
    properties.getMainThesis().setUrl("http://main-thesis");
    properties.getGenAi().setUrl("http://genai");

    RestClient mockedClient = restClientBuilder.build();

    service = new ScrapeCoordinationService(mockedClient, mockedClient, mockedClient, properties);
  }

  @Test
  void runScrapeCycle_FullPipelineSuccess() {
    // 1. Mock the endpoints fetch from Main Thesis Service
    mockServer
        .expect(requestTo("/internal/v1/thesis-service/source-endpoints"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{\"endpoints\":[{\"id\":1,\"chairId\":10,\"chairName\":\"AI"
                    + " Chair\",\"url\":\"http://chair.example.com/theses\"}]}",
                MediaType.APPLICATION_JSON));

    // 2. Mock fetching the raw HTML from the external website
    mockServer
        .expect(requestTo("http://chair.example.com/theses"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("<html><h1>AI Thesis</h1></html>", MediaType.TEXT_HTML));

    // 2b. Mock detect-changes call to Thesis Service
    mockServer
        .expect(requestTo("/thesis-internal/v1/source-endpoints/1/detect-changes"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                "{\"changed\":true,\"sanitizedHtml\":\"<html><h1>AI"
                    + " Thesis</h1></html>\",\"contentHash\":\"some-hash\"}",
                MediaType.APPLICATION_JSON));

    // 3. Mock the GenAI Python Service extraction POST request
    mockServer
        .expect(requestTo("/internal/v1/genai-service/extract-theses"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                "{\"theses\":[{\"title\":\"AI"
                    + " Thesis\",\"sourceUrl\":\"http://chair.example.com/theses\"}]}",
                MediaType.APPLICATION_JSON));

    // 4. Mock the final submission PUT request back to the Main Thesis Service
    mockServer
        .expect(requestTo("/internal/v1/thesis-service/source-endpoints/1/theses"))
        .andExpect(method(HttpMethod.PUT))
        .andRespond(withSuccess());

    // 5. Mock the final SUCCESS logging
    mockServer
        .expect(requestTo("/internal/v1/thesis-service/scrape-runs"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess());

    // Act
    service.runScrapeCycle();

    // Assert - ensures all the mocked server endpoints were hit in the exact order requested
    mockServer.verify();
  }

  @Test
  void runScrapeCycle_UnchangedPathShortCircuits() {
    // 1. Mock the endpoints fetch from Main Thesis Service
    mockServer
        .expect(requestTo("/internal/v1/thesis-service/source-endpoints"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{\"endpoints\":[{\"id\":1,\"chairId\":10,\"chairName\":\"AI"
                    + " Chair\",\"url\":\"http://chair.example.com/theses\"}]}",
                MediaType.APPLICATION_JSON));

    // 2. Mock fetching the raw HTML from the external website
    mockServer
        .expect(requestTo("http://chair.example.com/theses"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("<html><h1>AI Thesis</h1></html>", MediaType.TEXT_HTML));

    // 2b. Mock detect-changes call to Thesis Service with changed = false
    mockServer
        .expect(requestTo("/thesis-internal/v1/source-endpoints/1/detect-changes"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                "{\"changed\":false,\"sanitizedHtml\":\"<html><h1>AI"
                    + " Thesis</h1></html>\",\"contentHash\":\"some-hash\"}",
                MediaType.APPLICATION_JSON));

    // 3. Mock the final SUCCESS logging (since it should still log SUCCESS, but short-circuited)
    mockServer
        .expect(requestTo("/internal/v1/thesis-service/scrape-runs"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess());

    // Act
    service.runScrapeCycle();

    // Assert - ensures no other requests (like GenAI extraction or theses PUT) were made
    mockServer.verify();
  }
}
