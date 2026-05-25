package com.project95.thesis.scraping.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ScrapeCoordinationServiceTest {

    private MockRestServiceServer mockServer;
    private ScrapeCoordinationService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        service = new ScrapeCoordinationService(restClientBuilder.build(), "http://main-thesis", "http://genai");
    }

    @Test
    void runScrapeCycle_FullPipelineSuccess() {
        // 1. Mock the endpoints fetch from Main Thesis Service
        String endpointsJson = "{\"endpoints\":[{\"id\":1,\"chairId\":10,\"chairName\":\"AI Chair\",\"url\":\"http://chair.example.com/theses\"}]}";
        mockServer.expect(requestTo("http://main-thesis/internal/v1/thesis-service/source-endpoints"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(endpointsJson, MediaType.APPLICATION_JSON));

        // 2. Mock fetching the raw HTML from the external website
        mockServer.expect(requestTo("http://chair.example.com/theses"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("<html><h1>AI Thesis</h1></html>", MediaType.TEXT_HTML));

        // 3. Mock the GenAI Python Service extraction POST request
        String genAiJson = "{\"theses\":[{\"title\":\"AI Thesis\",\"sourceUrl\":\"http://chair.example.com/theses\"}]}";
        mockServer.expect(requestTo("http://genai/internal/v1/genai-service/extract-theses"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(genAiJson, MediaType.APPLICATION_JSON));

        // 4. Mock the final submission PUT request back to the Main Thesis Service
        mockServer.expect(requestTo("http://main-thesis/internal/v1/thesis-service/chairs/10/theses"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess());

        // Act
        service.runScrapeCycle();

        // Assert - ensures all the mocked server endpoints were hit in the exact order requested
        mockServer.verify();
    }
}
