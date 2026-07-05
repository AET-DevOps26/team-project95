package com.project95.thesis.thesis.contract;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OpenApiContractTest {

  private static final Path OPENAPI_SPEC = Path.of("..", "..", "api", "openapi-v1.yml");

  @Test
  void canonicalOpenApiContractParsesWithoutValidationErrors() {
    SwaggerParseResult result = parseOpenApiContract();

    assertThat(result.getOpenAPI()).isNotNull();
    assertThat(result.getMessages()).isEmpty();
  }

  @Test
  void frontendEndpointsDocumentExpectedOperationsAndStatusCodes() {
    OpenAPI openApi = openApi();

    assertOperation(
        openApi,
        "/api/v1/theses",
        PathItem.HttpMethod.GET,
        "listTheses",
        Map.of("200", "#/components/schemas/ThesisSearchResult"));
    assertOperation(
        openApi,
        "/api/v1/theses/search",
        PathItem.HttpMethod.POST,
        "searchTheses",
        Map.of("200", "#/components/schemas/SearchThesesResponse"));
    assertJsonRequestBody(
        openApi,
        "/api/v1/theses/search",
        PathItem.HttpMethod.POST,
        "#/components/schemas/SearchThesesRequest");
    assertOperation(
        openApi,
        "/api/v1/theses/{thesisId}",
        PathItem.HttpMethod.GET,
        "getThesisById",
        Map.of(
            "200", "#/components/schemas/ThesisProposal",
            "404", "#/components/schemas/ErrorResponse"));
    assertOperation(
        openApi,
        "/api/v1/chairs",
        PathItem.HttpMethod.GET,
        "listChairs",
        Map.of("200", "#/components/schemas/Chair"));
    assertOperation(
        openApi,
        "/api/v1/filters",
        PathItem.HttpMethod.GET,
        "getAvailableFilters",
        Map.of("200", "#/components/schemas/AvailableFiltersResponse"));
  }

  @Test
  void internalServiceEndpointsDocumentExpectedOperationsStatusCodesAndPayloads() {
    OpenAPI openApi = openApi();

    assertOperation(
        openApi,
        "/internal/v1/thesis-service/source-endpoints",
        PathItem.HttpMethod.GET,
        "listSourceEndpointsForScraping",
        Map.of("200", "#/components/schemas/SourceEndpointListResponse"));
    assertOperation(
        openApi,
        "/internal/v1/thesis-service/scrape-runs",
        PathItem.HttpMethod.POST,
        "logScrapeRun",
        Map.of("201", "#/components/schemas/ScrapeRunLogResponse"));
    assertJsonRequestBody(
        openApi,
        "/internal/v1/thesis-service/scrape-runs",
        PathItem.HttpMethod.POST,
        "#/components/schemas/ScrapeRunLogRequest");
    assertOperation(
        openApi,
        "/internal/v1/thesis-service/source-endpoints/{sourceEndpointId}/theses",
        PathItem.HttpMethod.PUT,
        "replaceChairTheses",
        Map.of("200", "#/components/schemas/SourceEndpointThesesReplacementResponse"));
    assertJsonRequestBody(
        openApi,
        "/internal/v1/thesis-service/source-endpoints/{sourceEndpointId}/theses",
        PathItem.HttpMethod.PUT,
        "#/components/schemas/SourceEndpointThesesReplacementRequest");
    assertOperation(
        openApi,
        "/internal/v1/vector-search-service/search",
        PathItem.HttpMethod.POST,
        "semanticSearch",
        Map.of("200", "#/components/schemas/VectorSearchResponse"));
    assertJsonRequestBody(
        openApi,
        "/internal/v1/vector-search-service/search",
        PathItem.HttpMethod.POST,
        "#/components/schemas/VectorSearchRequest");
    assertOperation(
        openApi,
        "/internal/v1/genai-service/extract-theses",
        PathItem.HttpMethod.POST,
        "extractThesesFromRawContent",
        Map.of("200", "#/components/schemas/GenAIExtractionResponse"));
    assertJsonRequestBody(
        openApi,
        "/internal/v1/genai-service/extract-theses",
        PathItem.HttpMethod.POST,
        "#/components/schemas/GenAIExtractionRequest");
  }

  @Test
  void coreSchemasKeepRequiredFieldsUsedByGeneratedDtosAndClients() {
    OpenAPI openApi = openApi();

    assertRequiredFields(openApi, "SearchThesesResponse", "items", "page", "size", "totalElements");
    assertRequiredFields(
        openApi, "ThesisProposal", "id", "chairId", "title", "sourceUrl", "status");
    assertRequiredFields(openApi, "SourceEndpoint", "id", "chairId", "url", "status");
    assertRequiredFields(
        openApi, "ScrapeRunLogRequest", "sourceEndpointId", "startedAt", "finishedAt", "status");
    assertRequiredFields(openApi, "SourceEndpointThesesReplacementRequest", "theses");
    assertRequiredFields(openApi, "VectorSearchRequest", "query");
    assertRequiredFields(
        openApi, "GenAIExtractionRequest", "sourceEndpointId", "chairId", "sourceUrl", "rawHtml");
  }

  private static SwaggerParseResult parseOpenApiContract() {
    ParseOptions options = new ParseOptions();
    options.setResolve(true);
    options.setResolveFully(false);
    return new OpenAPIParser().readLocation(OPENAPI_SPEC.toString(), null, options);
  }

  private static OpenAPI openApi() {
    SwaggerParseResult result = parseOpenApiContract();
    assertThat(result.getMessages()).isEmpty();
    return result.getOpenAPI();
  }

  private static void assertOperation(
      OpenAPI openApi,
      String path,
      PathItem.HttpMethod method,
      String operationId,
      Map<String, String> expectedResponseRefs) {
    Operation operation = operation(openApi, path, method);

    assertThat(operation.getOperationId()).isEqualTo(operationId);

    ApiResponses responses = operation.getResponses();
    assertThat(responses).containsKeys(expectedResponseRefs.keySet().toArray(String[]::new));
    expectedResponseRefs.forEach(
        (status, schemaRef) ->
            assertThat(jsonResponseSchemaRef(responses, status)).isEqualTo(schemaRef));
  }

  private static void assertJsonRequestBody(
      OpenAPI openApi, String path, PathItem.HttpMethod method, String expectedSchemaRef) {
    RequestBody requestBody = operation(openApi, path, method).getRequestBody();

    assertThat(requestBody).isNotNull();
    assertThat(requestBody.getRequired()).isTrue();
    assertThat(requestBody.getContent()).containsKey("application/json");
    assertThat(requestBody.getContent().get("application/json").getSchema().get$ref())
        .isEqualTo(expectedSchemaRef);
  }

  private static Operation operation(OpenAPI openApi, String path, PathItem.HttpMethod method) {
    assertThat(openApi.getPaths()).containsKey(path);

    Operation operation = openApi.getPaths().get(path).readOperationsMap().get(method);
    assertThat(operation).as("%s %s must be documented", method, path).isNotNull();
    return operation;
  }

  private static String jsonResponseSchemaRef(ApiResponses responses, String status) {
    Schema<?> schema = responses.get(status).getContent().get("application/json").getSchema();

    if (schema.get$ref() != null) {
      return schema.get$ref();
    }

    if ("array".equals(schema.getType())) {
      return schema.getItems().get$ref();
    }

    return null;
  }

  private static void assertRequiredFields(
      OpenAPI openApi, String schemaName, String... requiredFields) {
    Schema<?> schema = openApi.getComponents().getSchemas().get(schemaName);

    assertThat(schema).as("schema %s must exist", schemaName).isNotNull();
    assertThat(schema.getRequired()).contains(requiredFields);
  }
}
