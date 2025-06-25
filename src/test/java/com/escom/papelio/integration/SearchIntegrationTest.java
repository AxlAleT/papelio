package com.escom.papelio.integration;

import com.escom.papelio.dto.SearchRequestDTO;
import com.escom.papelio.model.SearchHistory;
import com.escom.papelio.repository.SearchHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql(scripts = "/sql/test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class SearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    public void searchRequestShouldReturnResultsAndSaveSearchHistory() throws Exception {
        // Create search request
        SearchRequestDTO searchRequest = new SearchRequestDTO();
        searchRequest.setQuery("artificial intelligence");
        searchRequest.setPage(1);
        searchRequest.setSize(10); // Fixed: using setSize instead of setPageSize

        // Perform search and verify response
        mockMvc.perform(post("/api/search")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("artificial intelligence"))
                .andExpect(jsonPath("$.page").value(1));

        // Verify search history was saved
        List<SearchHistory> searchHistories = searchHistoryRepository.findByUserEmailOrderBySearchDateDesc("test@example.com");
        boolean foundSearch = searchHistories.stream()
                .anyMatch(history -> "artificial intelligence".equals(history.getSearchQuery())); // Fixed: using getSearchQuery instead of getQuery
        assertTrue(foundSearch, "Search history was not saved correctly");
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    public void addAndRemoveFavoriteArticleShouldWorkCorrectly() throws Exception {
        // First add an article to favorites
        String articleJson = "{ \"articleId\": \"test-article-123\", \"title\": \"Test Article Title\" }";

        mockMvc.perform(post("/api/search/favorites")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(articleJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Article added to favorites"));

        // Check if article is in favorites
        mockMvc.perform(get("/api/search/favorites/check/test-article-123")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavorite").value(true));

        // Get favorites list
        mockMvc.perform(get("/api/search/favorites")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("test-article-123"))
                .andExpect(jsonPath("$.data[0].title").value("Test Article Title"));

        // Remove from favorites
        mockMvc.perform(post("/api/search/favorites/remove/test-article-123")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Article removed from favorites"));

        // Verify it's no longer in favorites
        mockMvc.perform(get("/api/search/favorites/check/test-article-123")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavorite").value(false));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    public void getArticleDetailsShouldTrackViewHistory() throws Exception {
        // View an article
        String articleId = "test-article-456";

        mockMvc.perform(get("/api/search/article/" + articleId)
                .with(csrf()))
                .andExpect(status().isOk());

        // View it again to test multiple views
        mockMvc.perform(get("/api/search/article/" + articleId)
                .with(csrf()))
                .andExpect(status().isOk());

        // Check view history endpoint
        mockMvc.perform(get("/api/user/history/views")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + articleId + "')]").exists());
    }
}
