package com.escom.papelio.service;

import com.escom.papelio.client.SemanticScholarApiClient;
import com.escom.papelio.dto.ArticleDTO;
import com.escom.papelio.dto.RecommendationRequestDTO;
import com.escom.papelio.dto.SearchRequestDTO;
import com.escom.papelio.dto.SearchResponseDTO;
import com.escom.papelio.mapper.SemanticScholarMapper;
import com.escom.papelio.model.SemanticScholarResponse;
import com.escom.papelio.model.SemanticScholarPaper;
import com.escom.papelio.model.SemanticScholarRecommendedPapers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SemanticScholarServiceTest {

    @Mock
    private SemanticScholarApiClient apiClient;

    @Mock
    private SemanticScholarMapper mapper;

    @InjectMocks
    private SemanticScholarService service;

    private SearchRequestDTO searchRequest;
    private SemanticScholarPaper mockPaper1;
    private SemanticScholarPaper mockPaper2;
    private ArticleDTO mockArticleDTO1;
    private ArticleDTO mockArticleDTO2;
    private SemanticScholarResponse mockResponse;

    private static final String TEST_QUERY = "artificial intelligence";
    private static final String TEST_PAPER_ID = "paper123";
    private static final String TARGET_FIELDS = "title,abstract,authors,venue,year,citationCount,url,externalIds";
    private static final String DETAILED_FIELDS = "title,abstract,authors,venue,year,citationCount,url,externalIds,references";

    @BeforeEach
    void setUp() {
        // Setup test data
        searchRequest = new SearchRequestDTO();
        searchRequest.setQuery(TEST_QUERY);
        searchRequest.setPage(0);
        searchRequest.setSize(10);

        // Setup mock papers
        mockPaper1 = new SemanticScholarPaper();
        mockPaper1.setPaperId("paper123");
        mockPaper1.setTitle("Test Paper 1");

        mockPaper2 = new SemanticScholarPaper();
        mockPaper2.setPaperId("paper456");
        mockPaper2.setTitle("Test Paper 2");

        // Setup mock DTOs
        mockArticleDTO1 = new ArticleDTO();
        mockArticleDTO1.setId("paper123");
        mockArticleDTO1.setTitle("Test Paper 1");

        mockArticleDTO2 = new ArticleDTO();
        mockArticleDTO2.setId("paper456");
        mockArticleDTO2.setTitle("Test Paper 2");

        // Setup mock API response
        mockResponse = new SemanticScholarResponse();
        mockResponse.setTotal(2L);
        mockResponse.setData(Arrays.asList(mockPaper1, mockPaper2));
    }

    @Test
    void searchArticles_Success() {
        // Arrange
        when(apiClient.searchPapers(eq(TEST_QUERY), eq(0), eq(100), eq(TARGET_FIELDS)))
                .thenReturn(mockResponse);
        when(mapper.mapToArticleDTO(mockPaper1)).thenReturn(mockArticleDTO1);
        when(mapper.mapToArticleDTO(mockPaper2)).thenReturn(mockArticleDTO2);

        // Act
        SearchResponseDTO result = service.searchArticles(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getArticles().size());
        assertEquals(2, result.getTotalResults());
        assertEquals(0, result.getCurrentPage());
        assertEquals(1, result.getTotalPages());
        assertEquals(TEST_QUERY, result.getQuery());

        // Verify the API was called with correct parameters
        verify(apiClient).searchPapers(eq(TEST_QUERY), eq(0), eq(100), eq(TARGET_FIELDS));
        verify(mapper, times(2)).mapToArticleDTO(any(SemanticScholarPaper.class));
    }

    @Test
    void searchArticles_ApiReturnsNull() {
        // Arrange
        when(apiClient.searchPapers(eq(TEST_QUERY), eq(0), eq(100), eq(TARGET_FIELDS)))
                .thenReturn(null);

        // Act
        SearchResponseDTO result = service.searchArticles(searchRequest);

        // Assert
        assertNotNull(result);
        assertTrue(result.getArticles().isEmpty());
        assertEquals(0, result.getTotalResults());
        assertEquals(0, result.getCurrentPage());
        assertEquals(0, result.getTotalPages());
        assertEquals(TEST_QUERY, result.getQuery());
    }

    @Test
    void searchArticles_ApiThrowsException() {
        // Arrange
        when(apiClient.searchPapers(eq(TEST_QUERY), eq(0), eq(100), eq(TARGET_FIELDS)))
                .thenThrow(new RuntimeException("API Error"));

        // Act
        SearchResponseDTO result = service.searchArticles(searchRequest);

        // Assert
        assertNotNull(result);
        assertTrue(result.getArticles().isEmpty());
        assertEquals(0, result.getTotalResults());
        assertEquals(0, result.getCurrentPage());
        assertEquals(0, result.getTotalPages());
        assertEquals(TEST_QUERY, result.getQuery());
    }

    @Test
    void getArticleById_Success() {
        // Arrange
        when(apiClient.getPaperById(eq(TEST_PAPER_ID), eq(DETAILED_FIELDS)))
                .thenReturn(mockPaper1);
        when(mapper.mapToArticleDTO(mockPaper1)).thenReturn(mockArticleDTO1);

        // Act
        Optional<ArticleDTO> result = service.getArticleById(TEST_PAPER_ID);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("paper123", result.get().getId());
        assertEquals("Test Paper 1", result.get().getTitle());

        verify(apiClient).getPaperById(eq(TEST_PAPER_ID), eq(DETAILED_FIELDS));
        verify(mapper).mapToArticleDTO(mockPaper1);
    }

    @Test
    void getArticleById_NotFound() {
        // Arrange
        when(apiClient.getPaperById(eq(TEST_PAPER_ID), eq(DETAILED_FIELDS)))
                .thenReturn(null);

        // Act
        Optional<ArticleDTO> result = service.getArticleById(TEST_PAPER_ID);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void getArticleById_ApiThrowsException() {
        // Arrange
        when(apiClient.getPaperById(eq(TEST_PAPER_ID), eq(DETAILED_FIELDS)))
                .thenThrow(new RuntimeException("API Error"));

        // Act
        Optional<ArticleDTO> result = service.getArticleById(TEST_PAPER_ID);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void getRecommendations_Success() {
        // Arrange
        List<String> paperIds = Arrays.asList("paper123", "paper456");
        RecommendationRequestDTO request = new RecommendationRequestDTO();
        request.setPaperIds(paperIds);

        SemanticScholarRecommendedPapers recommendationResponse = new SemanticScholarRecommendedPapers();
        recommendationResponse.setRecommendedPapers(Arrays.asList(mockPaper1, mockPaper2));

        when(apiClient.getRecommendations(eq(paperIds), eq(10), eq(TARGET_FIELDS)))
                .thenReturn(recommendationResponse);
        when(mapper.mapToArticleDTO(mockPaper1)).thenReturn(mockArticleDTO1);
        when(mapper.mapToArticleDTO(mockPaper2)).thenReturn(mockArticleDTO2);

        // Act
        SearchResponseDTO result = service.getRecommendations(request);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getArticles().size());
        assertEquals(2, result.getTotalResults());
        assertEquals(0, result.getCurrentPage());
        assertEquals(1, result.getTotalPages());
        assertEquals("recommendations", result.getQuery());

        verify(apiClient).getRecommendations(eq(paperIds), eq(10), eq(TARGET_FIELDS));
        verify(mapper, times(2)).mapToArticleDTO(any(SemanticScholarPaper.class));
    }

    @Test
    void getRecommendations_EmptyPaperIds() {
        // Arrange
        RecommendationRequestDTO request = new RecommendationRequestDTO();
        request.setPaperIds(Collections.emptyList());

        // Act
        SearchResponseDTO result = service.getRecommendations(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.getArticles().isEmpty());
        assertEquals(0, result.getTotalResults());

        // Verify API was not called
        verify(apiClient, never()).getRecommendations(anyList(), anyInt(), anyString());
    }

    @Test
    void getRecommendations_ApiReturnsNullRecommendations() {
        // Arrange
        List<String> paperIds = Collections.singletonList("paper123");
        RecommendationRequestDTO request = new RecommendationRequestDTO();
        request.setPaperIds(paperIds);

        SemanticScholarRecommendedPapers emptyResponse = new SemanticScholarRecommendedPapers();
        emptyResponse.setRecommendedPapers(null);

        when(apiClient.getRecommendations(eq(paperIds), eq(10), eq(TARGET_FIELDS)))
                .thenReturn(emptyResponse);

        // Act
        SearchResponseDTO result = service.getRecommendations(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.getArticles().isEmpty());
        assertEquals(0, result.getTotalResults());
    }

    @Test
    void getRecommendations_ApiThrowsException() {
        // Arrange
        List<String> paperIds = Collections.singletonList("paper123");
        RecommendationRequestDTO request = new RecommendationRequestDTO();
        request.setPaperIds(paperIds);

        when(apiClient.getRecommendations(eq(paperIds), eq(10), eq(TARGET_FIELDS)))
                .thenThrow(new RuntimeException("API Error"));

        // Act
        SearchResponseDTO result = service.getRecommendations(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.getArticles().isEmpty());
        assertEquals(0, result.getTotalResults());
    }

    @Test
    void getRecommendations_LimitResults() {
        // Arrange
        List<String> paperIds = Collections.singletonList("paper123");
        RecommendationRequestDTO request = new RecommendationRequestDTO();
        request.setPaperIds(paperIds);

        // Create more than TARGET_RECOMMENDATIONS papers
        List<SemanticScholarPaper> papers = Arrays.asList(
                mockPaper1, mockPaper2,
                new SemanticScholarPaper(), new SemanticScholarPaper(),
                new SemanticScholarPaper(), new SemanticScholarPaper(),
                new SemanticScholarPaper(), new SemanticScholarPaper(),
                new SemanticScholarPaper(), new SemanticScholarPaper(),
                new SemanticScholarPaper(), new SemanticScholarPaper() // 12 papers
        );

        SemanticScholarRecommendedPapers recommendationResponse = new SemanticScholarRecommendedPapers();
        recommendationResponse.setRecommendedPapers(papers);

        when(apiClient.getRecommendations(eq(paperIds), eq(10), eq(TARGET_FIELDS)))
                .thenReturn(recommendationResponse);

        // Setup mapper to return different ArticleDTOs for each paper
        for (int i = 0; i < papers.size(); i++) {
            ArticleDTO dto = new ArticleDTO();
            dto.setId("paper" + i);
            when(mapper.mapToArticleDTO(papers.get(i))).thenReturn(dto);
        }

        // Act
        SearchResponseDTO result = service.getRecommendations(request);

        // Assert
        assertNotNull(result);
        assertEquals(10, result.getArticles().size()); // Should be limited to 10
        assertEquals(10, result.getTotalResults());
    }
}