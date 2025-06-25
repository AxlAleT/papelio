package com.escom.papelio.service;

import com.escom.papelio.dto.ArticleDTO;
import com.escom.papelio.dto.SearchResponseDTO;
import com.escom.papelio.model.ArticleViewHistory;
import com.escom.papelio.repository.ArticleViewHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ArticleViewHistoryServiceTest {

    @Mock
    private ArticleViewHistoryRepository articleViewHistoryRepository;

    @InjectMocks
    private ArticleViewHistoryService articleViewHistoryService;

    @Captor
    private ArgumentCaptor<ArticleViewHistory> viewHistoryCaptor;

    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_ARTICLE_ID = "12345";
    private final String TEST_TITLE = "Test Article Title";
    private List<ArticleViewHistory> viewHistoryList;
    private List<Object[]> mostViewedArticles;

    @BeforeEach
    void setUp() {
        // Set up test data
        ArticleViewHistory history1 = new ArticleViewHistory();
        history1.setId(1L);
        history1.setUserEmail(TEST_EMAIL);
        history1.setArticleId(TEST_ARTICLE_ID);
        history1.setTitle(TEST_TITLE);
        history1.setViewDate(LocalDateTime.now().minusDays(1));

        ArticleViewHistory history2 = new ArticleViewHistory();
        history2.setId(2L);
        history2.setUserEmail(TEST_EMAIL);
        history2.setArticleId("67890");
        history2.setTitle("Another Article");
        history2.setViewDate(LocalDateTime.now().minusHours(2));

        viewHistoryList = Arrays.asList(history2, history1);

        // Set up most viewed articles data
        mostViewedArticles = Arrays.asList(
                new Object[]{TEST_TITLE, TEST_ARTICLE_ID, 10L},
                new Object[]{"Another Article", "67890", 5L}
        );
    }

    @Test
    void shouldSaveArticleViewSuccessfully() {
        // Act
        articleViewHistoryService.saveArticleView(TEST_EMAIL, TEST_ARTICLE_ID, TEST_TITLE);

        // Assert
        verify(articleViewHistoryRepository).save(viewHistoryCaptor.capture());
        ArticleViewHistory savedHistory = viewHistoryCaptor.getValue();

        assertEquals(TEST_EMAIL, savedHistory.getUserEmail());
        assertEquals(TEST_ARTICLE_ID, savedHistory.getArticleId());
        assertEquals(TEST_TITLE, savedHistory.getTitle());
        assertNotNull(savedHistory.getViewDate());
    }

    @Test
    void shouldRetrieveUserArticleViewHistory() {
        // Arrange
        when(articleViewHistoryRepository.findByUserEmailOrderByViewDateDesc(TEST_EMAIL))
                .thenReturn(viewHistoryList);

        // Act
        List<ArticleViewHistory> result = articleViewHistoryService.getUserArticleViewHistory(TEST_EMAIL);

        // Assert
        assertEquals(2, result.size());
        assertEquals(viewHistoryList, result);
        verify(articleViewHistoryRepository).findByUserEmailOrderByViewDateDesc(TEST_EMAIL);
    }

    @Test
    void shouldRetrieveMostViewedArticles() {
        // Arrange
        when(articleViewHistoryRepository.findMostViewedArticles()).thenReturn(mostViewedArticles);

        // Act
        List<Object[]> result = articleViewHistoryService.getMostViewedArticles();

        // Assert
        assertEquals(2, result.size());
        assertEquals(mostViewedArticles, result);
        verify(articleViewHistoryRepository).findMostViewedArticles();
    }

    @Test
    void shouldRetrieveMostViewedArticlesAsDTO() {
        // Arrange
        when(articleViewHistoryRepository.findMostViewedArticles()).thenReturn(mostViewedArticles);

        // Act
        SearchResponseDTO result = articleViewHistoryService.getMostViewedArticlesAsDTO();

        // Assert
        assertEquals(2, result.getData().size());
        assertEquals(2, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(1, result.getTotalPages());
        assertEquals("popular-articles", result.getQuery());

        // Check first article data
        ArticleDTO firstArticle = result.getData().get(0);
        assertEquals(TEST_TITLE, firstArticle.getTitle());
        assertEquals(TEST_ARTICLE_ID, firstArticle.getId());
    }

    @Test
    void shouldRetrieveUserViewedArticleIds() {
        // Arrange
        when(articleViewHistoryRepository.findByUserEmailOrderByViewDateDesc(TEST_EMAIL))
                .thenReturn(viewHistoryList);

        // Act
        List<String> result = articleViewHistoryService.getUserViewedArticleIds(TEST_EMAIL, 10);

        // Assert
        assertEquals(2, result.size());
        assertEquals("67890", result.get(0)); // Most recent first
        assertEquals(TEST_ARTICLE_ID, result.get(1));
    }

    @Test
    void shouldLimitReturnedUserViewedArticleIds() {
        // Arrange
        when(articleViewHistoryRepository.findByUserEmailOrderByViewDateDesc(TEST_EMAIL))
                .thenReturn(viewHistoryList);

        // Act
        List<String> result = articleViewHistoryService.getUserViewedArticleIds(TEST_EMAIL, 1);

        // Assert
        assertEquals(1, result.size());
        assertEquals("67890", result.get(0)); // Only the most recent
    }
}
