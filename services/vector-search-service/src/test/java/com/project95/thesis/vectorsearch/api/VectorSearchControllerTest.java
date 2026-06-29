package com.project95.thesis.vectorsearch.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project95.thesis.vectorsearch.util.ThesisVectorMetadata;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@SpringBootTest(
    properties = {
      "spring.ai.model.embedding=none",
      "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"
    })
@AutoConfigureMockMvc
class VectorSearchControllerTest {

  private static final String SEARCH_PATH = "/internal/v1/vector-search-service/search";
  private static final String INDEX_PATH =
      "/internal/v1/vector-search-service/source-endpoints/{sourceEndpointId}/index";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private VectorStore vectorStore;

  @TestConfiguration
  static class TestTransactionConfig {

    @Bean
    PlatformTransactionManager transactionManager() {
      return new AbstractPlatformTransactionManager() {
        @Override
        protected Object doGetTransaction() {
          return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {}

        @Override
        protected void doCommit(DefaultTransactionStatus status) {}

        @Override
        protected void doRollback(DefaultTransactionStatus status) {}
      };
    }
  }

  @Test
  void semanticSearchReturnsVectorResultsAndUsesSearchRequest() throws Exception {
    Document document =
        Document.builder()
            .id("doc-1001")
            .text("Semantic search thesis")
            .metadata(
                Map.of(ThesisVectorMetadata.THESIS_ID, 1001L, ThesisVectorMetadata.CHAIR_ID, 3L))
            .score(0.87D)
            .build();
    when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document));

    mockMvc
        .perform(
            post(SEARCH_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "query": "  semantic search with LLMs  ",
                      "filters": {
                        "chairIds": [3],
                        "degreeTypes": ["MASTER"],
                        "researchAreas": ["Artificial Intelligence"]
                      },
                      "limit": 5
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results[0].thesisId").value(1001))
        .andExpect(jsonPath("$.results[0].chairId").value(3))
        .andExpect(jsonPath("$.results[0].score").value(0.87));

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    verify(vectorStore).similaritySearch(captor.capture());
    SearchRequest searchRequest = captor.getValue();
    assertThat(searchRequest.getQuery()).isEqualTo("semantic search with LLMs");
    assertThat(searchRequest.getTopK()).isEqualTo(5);
    assertThat(searchRequest.hasFilterExpression()).isTrue();
  }

  @Test
  void semanticSearchReturnsEmptyResultsWhenVectorStoreFindsNothing() throws Exception {
    when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

    mockMvc
        .perform(
            post(SEARCH_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"non matching topic\",\"limit\":10}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results").isArray())
        .andExpect(jsonPath("$.results").isEmpty());

    verify(vectorStore).similaritySearch(any(SearchRequest.class));
  }

  @Test
  void semanticSearchRejectsBlankQueryWithoutCallingVectorStore() throws Exception {
    mockMvc
        .perform(
            post(SEARCH_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"   \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Search query must not be blank"));

    verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
  }

  @Test
  void indexSourceEndpointThesesReplacesExistingVectors() throws Exception {
    mockMvc
        .perform(
            post(INDEX_PATH, 7L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "scrapeRunId": 42,
                      "theses": [
                        {
                          "thesisId": 1001,
                          "chairId": 3,
                          "sourceEndpointId": 7,
                          "title": "Semantic Search for Thesis Discovery",
                          "degreeType": "MASTER",
                          "aiOverview": "Build and evaluate semantic search.",
                          "originalDescription": "Investigate semantic retrieval for theses.",
                          "researchArea": "Artificial Intelligence",
                          "sourceUrl": "https://example-chair.tum.de/theses/semantic-search",
                          "tags": ["Semantic Search", "LLM"]
                        }
                      ]
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sourceEndpointId").value(7))
        .andExpect(jsonPath("$.deletedVectorEntries").value(0))
        .andExpect(jsonPath("$.insertedVectorEntries").value(1));

    verify(vectorStore).delete(any(Expression.class));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Document>> documentsCaptor = ArgumentCaptor.forClass(List.class);
    verify(vectorStore).add(documentsCaptor.capture());
    List<Document> documents = documentsCaptor.getValue();
    assertThat(documents).hasSize(1);
    assertThat(documents.getFirst().getText()).contains("Semantic Search for Thesis Discovery");
    assertThat(documents.getFirst().getMetadata())
        .containsEntry(ThesisVectorMetadata.THESIS_ID, 1001L)
        .containsEntry(ThesisVectorMetadata.SOURCE_ENDPOINT_ID, 7L);
  }

  @Test
  void indexSourceEndpointThesesDeletesButSkipsAddForEmptyReplacement() throws Exception {
    mockMvc
        .perform(
            post(INDEX_PATH, 7L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"scrapeRunId\":42,\"theses\":[]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sourceEndpointId").value(7))
        .andExpect(jsonPath("$.insertedVectorEntries").value(0));

    verify(vectorStore).delete(any(Expression.class));
    verify(vectorStore, never()).add(anyList());
  }

  @Test
  void indexSourceEndpointThesesRejectsNullThesesWithoutCallingVectorStore() throws Exception {
    mockMvc
        .perform(
            post(INDEX_PATH, 7L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"scrapeRunId\":42,\"theses\":null}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Request validation failed"));

    verify(vectorStore, never()).delete(any(Expression.class));
    verify(vectorStore, never()).add(anyList());
  }
}
