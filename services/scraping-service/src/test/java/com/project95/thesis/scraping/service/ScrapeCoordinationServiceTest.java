package com.project95.thesis.scraping.service;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
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

  @Test
  void runScrapeCycle_ChairWebsiteRequestFails_LogsFailedRun() {
    // 1. Mock the endpoints fetch from Main Thesis Service
    mockServer
        .expect(requestTo("/internal/v1/thesis-service/source-endpoints"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{\"endpoints\":[{\"id\":1,\"chairId\":10,\"chairName\":\"AI"
                    + " Chair\",\"url\":\"http://chair.example.com/theses\"}]}",
                MediaType.APPLICATION_JSON));

    // 2. Mock fetching the raw HTML from the external website to FAIL (500)
    mockServer
        .expect(requestTo("http://chair.example.com/theses"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    // 3. Mock the final FAILURE logging POST request
    mockServer
        .expect(requestTo("/internal/v1/thesis-service/scrape-runs"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().string(containsString("\"status\":\"FAILED\"")))
        .andExpect(content().string(containsString("\"errorMessage\":\"500 Internal Server Error")))
        .andRespond(withSuccess());

    // Act
    service.runScrapeCycle();

    // Assert
    mockServer.verify();
  }

  @Test
  void runScrapeCycle_GenAiReturnsEmptyResponse_LogsFailedRun() {
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

    // 3. Mock the GenAI Python Service extraction POST request returning null theses list
    mockServer
        .expect(requestTo("/internal/v1/genai-service/extract-theses"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("{\"theses\":null}", MediaType.APPLICATION_JSON));

    // 4. Mock the final FAILURE logging POST request
    mockServer
        .expect(requestTo("/internal/v1/thesis-service/scrape-runs"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().string(containsString("\"status\":\"FAILED\"")))
        .andExpect(
            content()
                .string(
                    containsString("\"errorMessage\":\"GenAI extraction returned null response\"")))
        .andExpect(
            content()
                .string(containsString("\"rawHtmlSnapshot\":\"<html><h1>AI Thesis</h1></html>\"")))
        .andExpect(content().string(containsString("\"candidatesFound\":0")))
        .andRespond(withSuccess());

    // Act
    service.runScrapeCycle();

    // Assert
    mockServer.verify();
  }

  @Test
  void runScrapeCycle_GenAiUnavailable_LogsFailedRun() {
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

    // 3. Mock the GenAI Python Service extraction POST request failing (500)
    mockServer
        .expect(requestTo("/internal/v1/genai-service/extract-theses"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withServerError());

    // 4. Mock the final FAILURE logging POST request
    mockServer
        .expect(requestTo("/internal/v1/thesis-service/scrape-runs"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().string(containsString("\"status\":\"FAILED\"")))
        .andExpect(content().string(containsString("\"errorMessage\":\"500 Internal Server Error")))
        .andExpect(
            content()
                .string(containsString("\"rawHtmlSnapshot\":\"<html><h1>AI Thesis</h1></html>\"")))
        .andExpect(content().string(containsString("\"candidatesFound\":0")))
        .andRespond(withSuccess());

    // Act
    service.runScrapeCycle();

    // Assert
    mockServer.verify();
  }

  @Test
  void runScrapeCycle_ThesisSubmissionFails_LogsFailedRun() {
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

    // 4. Mock the final submission PUT request back to the Main Thesis Service failing (500)
    mockServer
        .expect(requestTo("/internal/v1/thesis-service/source-endpoints/1/theses"))
        .andExpect(method(HttpMethod.PUT))
        .andRespond(withServerError());

    // 5. Mock the final FAILURE logging POST request
    mockServer
        .expect(requestTo("/internal/v1/thesis-service/scrape-runs"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().string(containsString("\"status\":\"FAILED\"")))
        .andExpect(content().string(containsString("\"errorMessage\":\"500 Internal Server Error")))
        .andExpect(
            content()
                .string(containsString("\"rawHtmlSnapshot\":\"<html><h1>AI Thesis</h1></html>\"")))
        .andExpect(content().string(containsString("\"candidatesFound\":0")))
        .andRespond(withSuccess());

    // Act
    service.runScrapeCycle();

    // Assert
    mockServer.verify();
  }

  @Test
  void runScrapeCycle_EndpointListIsEmpty_DoesNotProcess() {
    // 1. Mock the endpoints fetch to return an empty list
    mockServer
        .expect(requestTo("/internal/v1/thesis-service/source-endpoints"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("{\"endpoints\":[]}", MediaType.APPLICATION_JSON));

    // Act
    service.runScrapeCycle();

    // Assert
    mockServer.verify();
  }

  @Test
  void runScrapeCycle_FetchEndpointsFails_GracefullyExits() {
    // 1. Mock the endpoints fetch to fail (500)
    mockServer
        .expect(requestTo("/internal/v1/thesis-service/source-endpoints"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    // Act
    service.runScrapeCycle();

    // Assert - verifies that we exit immediately and perform no other network calls
    mockServer.verify();
  }

  @Test
  void runScrapeCycle_ChairWebsiteReturnsEmptyHtml_LogsFailedRun() {
    // 1. Mock the endpoints fetch from Main Thesis Service
    mockServer
        .expect(requestTo("/internal/v1/thesis-service/source-endpoints"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{\"endpoints\":[{\"id\":1,\"chairId\":10,\"chairName\":\"AI"
                    + " Chair\",\"url\":\"http://chair.example.com/theses\"}]}",
                MediaType.APPLICATION_JSON));

    // 2. Mock fetching the raw HTML from the external website to return empty string
    mockServer
        .expect(requestTo("http://chair.example.com/theses"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("", MediaType.TEXT_HTML));

    // 3. Mock the final FAILURE logging POST request
    mockServer
        .expect(requestTo("/internal/v1/thesis-service/scrape-runs"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().string(containsString("\"status\":\"FAILED\"")))
        .andExpect(
            content()
                .string(containsString("\"errorMessage\":\"Received empty HTML from source URL\"")))
        .andExpect(content().string(containsString("\"candidatesFound\":0")))
        .andRespond(withSuccess());

    // Act
    service.runScrapeCycle();

    // Assert
    mockServer.verify();
  }

  @Test
  void runScrapeCycle_MultipleEndpoints_OneFailsOneSucceeds() {
    // 1. Mock endpoints list response with two endpoints
    mockServer
        .expect(requestTo("/internal/v1/thesis-service/source-endpoints"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{\"endpoints\":[{\"id\":1,\"chairId\":10,\"chairName\":\"AI"
                    + " Chair\",\"url\":\"http://chair1.example.com/theses\"},{\"id\":2,\"chairId\":20,\"chairName\":\"DB"
                    + " Chair\",\"url\":\"http://chair2.example.com/theses\"}]}",
                MediaType.APPLICATION_JSON));

    // -- ENDPOINT 1 (Fails to fetch HTML) --
    // 2. Mock fetching HTML for endpoint 1 to fail (500)
    mockServer
        .expect(requestTo("http://chair1.example.com/theses"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    // 3. Mock logging failure for endpoint 1
    mockServer
        .expect(requestTo("/internal/v1/thesis-service/scrape-runs"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().string(containsString("\"sourceEndpointId\":1")))
        .andExpect(content().string(containsString("\"status\":\"FAILED\"")))
        .andRespond(withSuccess());

    // -- ENDPOINT 2 (Succeeds) --
    // 4. Mock fetching HTML for endpoint 2
    mockServer
        .expect(requestTo("http://chair2.example.com/theses"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("<html><h1>DB Thesis</h1></html>", MediaType.TEXT_HTML));

    // 4b. Mock detect-changes call for endpoint 2
    mockServer
        .expect(requestTo("/thesis-internal/v1/source-endpoints/2/detect-changes"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                "{\"changed\":true,\"sanitizedHtml\":\"<html><h1>DB"
                    + " Thesis</h1></html>\",\"contentHash\":\"some-hash\"}",
                MediaType.APPLICATION_JSON));

    // 5. Mock GenAI extraction for endpoint 2
    mockServer
        .expect(requestTo("/internal/v1/genai-service/extract-theses"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                "{\"theses\":[{\"title\":\"DB"
                    + " Thesis\",\"sourceUrl\":\"http://chair2.example.com/theses\"}]}",
                MediaType.APPLICATION_JSON));

    // 6. Mock submission for endpoint 2
    mockServer
        .expect(requestTo("/internal/v1/thesis-service/source-endpoints/2/theses"))
        .andExpect(method(HttpMethod.PUT))
        .andRespond(withSuccess());

    // 7. Mock logging success for endpoint 2
    mockServer
        .expect(requestTo("/internal/v1/thesis-service/scrape-runs"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().string(containsString("\"sourceEndpointId\":2")))
        .andExpect(content().string(containsString("\"status\":\"SUCCESS\"")))
        .andExpect(content().string(containsString("\"candidatesFound\":1")))
        .andRespond(withSuccess());

    // Act
    service.runScrapeCycle();

    // Assert
    mockServer.verify();
  }

  @Test
  void runScrapeCycle_DetectChangesFails_LogsFailedRun() {
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

    // 2b. Mock detect-changes call to Thesis Service to fail (500)
    mockServer
        .expect(requestTo("/thesis-internal/v1/source-endpoints/1/detect-changes"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withServerError());

    // 3. Mock the final FAILURE logging POST request
    mockServer
        .expect(requestTo("/internal/v1/thesis-service/scrape-runs"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().string(containsString("\"status\":\"FAILED\"")))
        .andExpect(content().string(containsString("\"errorMessage\":\"500 Internal Server Error")))
        .andExpect(
            content()
                .string(containsString("\"rawHtmlSnapshot\":\"<html><h1>AI Thesis</h1></html>\"")))
        .andExpect(content().string(containsString("\"candidatesFound\":0")))
        .andRespond(withSuccess());

    // Act
    service.runScrapeCycle();

    // Assert
    mockServer.verify();
  }
}
