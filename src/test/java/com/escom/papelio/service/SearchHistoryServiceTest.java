package com.escom.papelio.service;

import com.escom.papelio.model.SearchHistory;
import com.escom.papelio.repository.SearchHistoryRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SearchHistoryServiceTest {

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @InjectMocks
    private SearchHistoryService searchHistoryService;

    @Captor
    private ArgumentCaptor<SearchHistory> searchHistoryCaptor;

    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_QUERY = "machine learning";

    @BeforeEach
    void setUp() {
        // Common setup for tests
    }

    @Test
    void shouldSaveSearchQuerySuccessfully() {
        // Act
        searchHistoryService.saveSearchQuery(TEST_EMAIL, TEST_QUERY);

        // Assert
        verify(searchHistoryRepository).save(searchHistoryCaptor.capture());
        SearchHistory savedHistory = searchHistoryCaptor.getValue();

        assertEquals(TEST_EMAIL, savedHistory.getUserEmail());
        assertEquals(TEST_QUERY, savedHistory.getSearchQuery());
        assertNotNull(savedHistory.getSearchDate());
    }

    @Test
    void shouldRetrieveUserSearchHistory() {
        // Arrange
        SearchHistory history1 = new SearchHistory();
        history1.setUserEmail(TEST_EMAIL);
        history1.setSearchQuery("artificial intelligence");
        history1.setSearchDate(LocalDateTime.now().minusDays(1));

        SearchHistory history2 = new SearchHistory();
        history2.setUserEmail(TEST_EMAIL);
        history2.setSearchQuery("neural networks");
        history2.setSearchDate(LocalDateTime.now().minusHours(2));

        List<SearchHistory> expectedHistory = Arrays.asList(history2, history1);

        when(searchHistoryRepository.findByUserEmailOrderBySearchDateDesc(TEST_EMAIL))
                .thenReturn(expectedHistory);

        // Act
        List<SearchHistory> result = searchHistoryService.getUserSearchHistory(TEST_EMAIL);

        // Assert
        assertEquals(2, result.size());
        assertEquals(expectedHistory, result);
        verify(searchHistoryRepository).findByUserEmailOrderBySearchDateDesc(TEST_EMAIL);
    }

    @Test
    void shouldRetrieveTopSearchQueries() {
        // Arrange
        Object[] query1 = new Object[]{"machine learning", 10L};
        Object[] query2 = new Object[]{"deep learning", 7L};
        List<Object[]> expectedTopQueries = Arrays.asList(query1, query2);

        when(searchHistoryRepository.findTopSearchQueries()).thenReturn(expectedTopQueries);

        // Act
        List<Object[]> result = searchHistoryService.getTopSearchQueries();

        // Assert
        assertEquals(2, result.size());
        assertEquals(expectedTopQueries, result);
        verify(searchHistoryRepository).findTopSearchQueries();
    }
}
