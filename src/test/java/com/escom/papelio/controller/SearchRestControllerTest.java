package com.escom.papelio.controller;

import com.escom.papelio.dto.ArticleFavoriteRequestDTO;
import com.escom.papelio.dto.RecommendationRequestDTO;
import com.escom.papelio.dto.SearchRequestDTO;
import com.escom.papelio.dto.SearchResponseDTO;
import com.escom.papelio.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SearchRestControllerTest {

    @Mock
    private ArticleService articleService;

    @Mock
    private SearchHistoryService searchHistoryService;

    @Mock
    private ArticleViewHistoryService articleViewHistoryService;

    @Mock
    private SemanticScholarService semanticScholarService;

    @Mock
    private ArticleFavoriteService articleFavoriteService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SearchRestController searchRestController;

    private SearchRequestDTO searchRequestDTO;
    private RecommendationRequestDTO recommendationRequestDTO;
    private ArticleFavoriteRequestDTO articleFavoriteRequestDTO;
    private SearchResponseDTO searchResponseDTO;

    @BeforeEach
    void setUp() {
        searchRequestDTO = new SearchRequestDTO();
        searchRequestDTO.setQuery("machine learning");
        searchRequestDTO.setPage(1);
        searchRequestDTO.setPageSize(10);

        recommendationRequestDTO = new RecommendationRequestDTO();
        recommendationRequestDTO.setPaperId("paper123");

        articleFavoriteRequestDTO = new ArticleFavoriteRequestDTO();
        articleFavoriteRequestDTO.setArticleId("article123");
        articleFavoriteRequestDTO.setTitle("Sample Article Title");

        searchResponseDTO = new SearchResponseDTO(Collections.emptyList(), 0, 1, 0, "query");

        when(authentication.getName()).thenReturn("user@example.com");
    }

    @Test
    void basicSearchShouldReturnSearchResults() {
        // Arrange
        when(articleService.searchArticles(any(SearchRequestDTO.class))).thenReturn(searchResponseDTO);

        // Act
        ResponseEntity<SearchResponseDTO> response = searchRestController.basicSearch(searchRequestDTO, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(searchHistoryService).saveSearchQuery(anyString(), anyString());
    }

    @Test
    void getRecommendationsShouldReturnRecommendedArticles() {
        // Arrange
        when(semanticScholarService.getRecommendations(anyString())).thenReturn(searchResponseDTO);

        // Act
        ResponseEntity<SearchResponseDTO> response = searchRestController.getRecommendations(
                recommendationRequestDTO, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getArticleDetailsShouldSaveViewHistoryAndReturnDetails() {
        // Arrange
        when(articleService.getArticleDetails(anyString())).thenReturn(Collections.emptyMap());

        // Act
        ResponseEntity<Map<String, Object>> response = searchRestController.getArticleDetails(
                "article123", authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(articleViewHistoryService).saveArticleView(anyString(), anyString());
    }

    @Test
    void addFavoriteShouldReturnSuccessWhenNewFavorite() {
        // Arrange
        when(articleFavoriteService.saveArticleFavorite(anyString(), anyString(), anyString())).thenReturn(true);

        // Act
        ResponseEntity<Map<String, Object>> response = searchRestController.addFavorite(
                articleFavoriteRequestDTO, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals("Article added to favorites", response.getBody().get("message"));
    }

    @Test
    void addFavoriteShouldReturnSuccessWhenAlreadyFavorite() {
        // Arrange
        when(articleFavoriteService.saveArticleFavorite(anyString(), anyString(), anyString())).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = searchRestController.addFavorite(
                articleFavoriteRequestDTO, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals("Article already in favorites", response.getBody().get("message"));
    }

    @Test
    void removeFavoriteShouldReturnSuccessWhenRemoved() {
        // Arrange
        when(articleFavoriteService.removeArticleFavorite(anyString(), anyString())).thenReturn(true);

        // Act
        ResponseEntity<Map<String, Object>> response = searchRestController.removeFavorite(
                "article123", authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals("Article removed from favorites", response.getBody().get("message"));
    }

    @Test
    void removeFavoriteShouldReturnSuccessWhenNotInFavorites() {
        // Arrange
        when(articleFavoriteService.removeArticleFavorite(anyString(), anyString())).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = searchRestController.removeFavorite(
                "article123", authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals("Article was not in favorites", response.getBody().get("message"));
    }

    @Test
    void checkFavoriteShouldReturnCorrectStatus() {
        // Arrange
        when(articleFavoriteService.isArticleFavorite(anyString(), anyString())).thenReturn(true);

        // Act
        ResponseEntity<Map<String, Object>> response = searchRestController.checkFavorite(
                "article123", authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("isFavorite"));
    }

    @Test
    void getFavoritesShouldReturnUserFavorites() {
        // Arrange
        when(articleFavoriteService.getUserFavoritesAsDTO(anyString())).thenReturn(searchResponseDTO);

        // Act
        ResponseEntity<SearchResponseDTO> response = searchRestController.getFavorites(authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(articleFavoriteService).getUserFavoritesAsDTO(anyString());
    }
}
