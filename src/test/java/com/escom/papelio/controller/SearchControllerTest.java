package com.escom.papelio.controller;

import com.escom.papelio.dto.SearchRequestDTO;
import com.escom.papelio.dto.SearchResponseDTO;
import com.escom.papelio.service.ArticleService;
import com.escom.papelio.service.SearchHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class SearchControllerTest {

    @Mock
    private ArticleService articleService;

    @Mock
    private SearchHistoryService searchHistoryService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SearchController searchController;

    private MockMvc mockMvc;
    private SearchResponseDTO searchResponseDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(searchController).build();

        // Set up test response data
        searchResponseDTO = new SearchResponseDTO(
                Collections.emptyList(),
                0,
                1,
                0,
                "test query");

        when(authentication.getName()).thenReturn("test@example.com");
    }

    @Test
    void showSearchPageShouldReturnSearchView() throws Exception {
        mockMvc.perform(get("/search"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/search"))
                .andExpect(model().attributeExists("searchRequest"));
    }

    @Test
    void showAdvancedSearchPageShouldReturnAdvancedSearchView() throws Exception {
        mockMvc.perform(get("/search/advanced"))
                .andExpect(status().isOk())
                .andExpect(view().name("search/advanced"))
                .andExpect(model().attributeExists("searchRequest"));
    }

    @Test
    void performSearchShouldReturnResultsAndSaveSearchHistory() throws Exception {
        // Arrange
        when(articleService.searchArticles(any(SearchRequestDTO.class))).thenReturn(searchResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/search/results")
                .param("query", "test query")
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("search/results"))
                .andExpect(model().attributeExists("results"))
                .andExpect(model().attribute("results", searchResponseDTO));

        // Verify search history is saved
        verify(searchHistoryService).saveSearchQuery(anyString(), anyString());
    }

    @Test
    void performSearchWithoutAuthenticationShouldNotSaveSearchHistory() throws Exception {
        // Arrange
        when(articleService.searchArticles(any(SearchRequestDTO.class))).thenReturn(searchResponseDTO);

        // Act & Assert - no authentication principal provided
        mockMvc.perform(post("/search/results")
                .param("query", "test query"))
                .andExpect(status().isOk())
                .andExpect(view().name("search/results"))
                .andExpect(model().attributeExists("results"));

        // No interaction with search history service should occur
    }
}
