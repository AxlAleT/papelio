package com.escom.papelio.service;

import com.escom.papelio.dto.SearchResponseDTO;
import com.escom.papelio.model.ArticleFavorite;
import com.escom.papelio.repository.ArticleFavoriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ArticleFavoriteServiceTest {

    @Mock
    private ArticleFavoriteRepository articleFavoriteRepository;

    @InjectMocks
    private ArticleFavoriteService articleFavoriteService;

    private final String USER_EMAIL = "test@example.com";
    private final String ARTICLE_ID = "1234567890";
    private final String ARTICLE_TITLE = "Test Article Title";

    @BeforeEach
    void setUp() {
        // Common setup for tests
    }

    @Test
    void shouldSaveArticleFavoriteSuccessfully() {
        // Arrange
        when(articleFavoriteRepository.existsByUserEmailAndArticleId(USER_EMAIL, ARTICLE_ID))
                .thenReturn(false);
        when(articleFavoriteRepository.save(any(ArticleFavorite.class))).thenReturn(new ArticleFavorite());

        // Act
        boolean result = articleFavoriteService.saveArticleFavorite(USER_EMAIL, ARTICLE_ID, ARTICLE_TITLE);

        // Assert
        assertTrue(result);
        verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
    }

    @Test
    void shouldNotSaveArticleFavoriteWhenAlreadyExists() {
        // Arrange
        when(articleFavoriteRepository.existsByUserEmailAndArticleId(USER_EMAIL, ARTICLE_ID))
                .thenReturn(true);

        // Act
        boolean result = articleFavoriteService.saveArticleFavorite(USER_EMAIL, ARTICLE_ID, ARTICLE_TITLE);

        // Assert
        assertFalse(result);
        verify(articleFavoriteRepository, never()).save(any(ArticleFavorite.class));
    }

    @Test
    void shouldRemoveArticleFavoriteSuccessfully() {
        // Arrange
        when(articleFavoriteRepository.existsByUserEmailAndArticleId(USER_EMAIL, ARTICLE_ID))
                .thenReturn(true);

        // Act
        boolean result = articleFavoriteService.removeArticleFavorite(USER_EMAIL, ARTICLE_ID);

        // Assert
        assertTrue(result);
        verify(articleFavoriteRepository).deleteByUserEmailAndArticleId(USER_EMAIL, ARTICLE_ID);
    }

    @Test
    void shouldNotRemoveArticleFavoriteWhenNotExists() {
        // Arrange
        when(articleFavoriteRepository.existsByUserEmailAndArticleId(USER_EMAIL, ARTICLE_ID))
                .thenReturn(false);

        // Act
        boolean result = articleFavoriteService.removeArticleFavorite(USER_EMAIL, ARTICLE_ID);

        // Assert
        assertFalse(result);
        verify(articleFavoriteRepository, never()).deleteByUserEmailAndArticleId(anyString(), anyString());
    }

    @Test
    void shouldCheckIfArticleIsFavorite() {
        // Arrange
        when(articleFavoriteRepository.existsByUserEmailAndArticleId(USER_EMAIL, ARTICLE_ID))
                .thenReturn(true);

        // Act
        boolean result = articleFavoriteService.isArticleFavorite(USER_EMAIL, ARTICLE_ID);

        // Assert
        assertTrue(result);
    }

    @Test
    void shouldReturnUserFavoritesAsDTO() {
        // Arrange
        ArticleFavorite favorite1 = new ArticleFavorite();
        favorite1.setArticleId(ARTICLE_ID);
        favorite1.setTitle(ARTICLE_TITLE);
        favorite1.setUserEmail(USER_EMAIL);
        favorite1.setFavoriteDate(LocalDateTime.now());

        ArticleFavorite favorite2 = new ArticleFavorite();
        favorite2.setArticleId("9876543210");
        favorite2.setTitle("Another Article");
        favorite2.setUserEmail(USER_EMAIL);
        favorite2.setFavoriteDate(LocalDateTime.now().minusDays(1));

        List<ArticleFavorite> favorites = Arrays.asList(favorite1, favorite2);

        when(articleFavoriteRepository.findByUserEmailOrderByFavoriteDateDesc(USER_EMAIL))
                .thenReturn(favorites);

        // Act
        SearchResponseDTO result = articleFavoriteService.getUserFavoritesAsDTO(USER_EMAIL);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotal());
        assertEquals(2, result.getData().size());
        assertEquals(ARTICLE_ID, result.getData().get(0).getId());
        assertEquals(ARTICLE_TITLE, result.getData().get(0).getTitle());
        assertEquals("9876543210", result.getData().get(1).getId());
        assertEquals("Another Article", result.getData().get(1).getTitle());
    }
}
