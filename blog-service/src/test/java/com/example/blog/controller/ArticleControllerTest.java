package com.example.blog.controller;

import com.example.blog.dto.ArticleDto;
import com.example.blog.dto.ArticleSummaryDto;
import com.example.blog.exception.ResourceNotFoundException;
import com.example.blog.service.ArticleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArticleController.class)
class ArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArticleService articleService;

    @Autowired
    private ObjectMapper objectMapper;

    private ArticleDto testArticleDto;
    private ArticleSummaryDto testArticleSummaryDto;

    @BeforeEach
    void setUp() {
        testArticleDto = new ArticleDto();
        testArticleDto.setId(1L);
        testArticleDto.setTitle("Test Article");
        testArticleDto.setContent("Test Content");
        testArticleDto.setTags("test,article");
        testArticleDto.setPublishStatus("PUBLISHED");

        testArticleSummaryDto = new ArticleSummaryDto();
        testArticleSummaryDto.setId(1L);
        testArticleSummaryDto.setTitle("Test Article");
    }

    @Test
    void testGetAllArticlesSuccess() throws Exception {
        // Arrange
        Page<ArticleSummaryDto> page = new PageImpl<>(
            List.of(testArticleSummaryDto),
            PageRequest.of(0, 10),
            1
        );
        when(articleService.getAllArticles(null, PageRequest.of(0, 10)))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/articles")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].id", equalTo(1)))
            .andExpect(jsonPath("$.content[0].title", equalTo("Test Article")));

        verify(articleService, times(1)).getAllArticles(any(), any());
    }

    @Test
    void testGetAllArticlesWithTitleFilter() throws Exception {
        // Arrange
        Page<ArticleSummaryDto> page = new PageImpl<>(
            List.of(testArticleSummaryDto),
            PageRequest.of(0, 10),
            1
        );
        when(articleService.getAllArticles("Test", PageRequest.of(0, 10)))
            .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/articles")
                .param("title", "Test")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)));

        verify(articleService, times(1)).getAllArticles("Test", any());
    }

    @Test
    void testGetAllArticlesInvalidPageNumber() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/articles")
                .param("page", "-1")
                .param("size", "10"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error", containsString("Page number")));
    }

    @Test
    void testGetAllArticlesInvalidPageSize() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/articles")
                .param("page", "0")
                .param("size", "150"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error", containsString("Page size")));
    }

    @Test
    void testGetAllArticlesPageSizeZero() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/articles")
                .param("page", "0")
                .param("size", "0"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testGetArticleSuccess() throws Exception {
        // Arrange
        when(articleService.getArticle(1L)).thenReturn(testArticleDto);

        // Act & Assert
        mockMvc.perform(get("/api/articles/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", equalTo(1)))
            .andExpect(jsonPath("$.title", equalTo("Test Article")))
            .andExpect(jsonPath("$.content", equalTo("Test Content")));

        verify(articleService, times(1)).getArticle(1L);
    }

    @Test
    void testGetArticleNotFound() throws Exception {
        // Arrange
        when(articleService.getArticle(999L))
            .thenThrow(new ResourceNotFoundException("Article not found"));

        // Act & Assert
        mockMvc.perform(get("/api/articles/999"))
            .andExpect(status().isNotFound());

        verify(articleService, times(1)).getArticle(999L);
    }

    @Test
    void testGetArticleInvalidId() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/articles/0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error", containsString("positive")));
    }

    @Test
    void testCreateArticleSuccess() throws Exception {
        // Arrange
        when(articleService.createArticle(any(ArticleDto.class)))
            .thenReturn(testArticleDto);

        // Act & Assert
        mockMvc.perform(post("/api/articles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testArticleDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", equalTo(1)))
            .andExpect(jsonPath("$.title", equalTo("Test Article")));

        verify(articleService, times(1)).createArticle(any(ArticleDto.class));
    }

    @Test
    void testCreateArticleWithoutTitle() throws Exception {
        // Arrange
        ArticleDto invalidDto = new ArticleDto();
        invalidDto.setContent("Test Content");

        // Act & Assert
        mockMvc.perform(post("/api/articles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateArticleSuccess() throws Exception {
        // Arrange
        ArticleDto updateDto = new ArticleDto();
        updateDto.setTitle("Updated Title");
        updateDto.setContent("Updated Content");

        when(articleService.updateArticle(eq(1L), any(ArticleDto.class)))
            .thenReturn(updateDto);

        // Act & Assert
        mockMvc.perform(put("/api/articles/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title", equalTo("Updated Title")));

        verify(articleService, times(1)).updateArticle(eq(1L), any(ArticleDto.class));
    }

    @Test
    void testUpdateArticleNotFound() throws Exception {
        // Arrange
        when(articleService.updateArticle(eq(999L), any(ArticleDto.class)))
            .thenThrow(new ResourceNotFoundException("Article not found"));

        // Act & Assert
        mockMvc.perform(put("/api/articles/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testArticleDto)))
            .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteArticleSuccess() throws Exception {
        // Arrange
        doNothing().when(articleService).deleteArticle(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/articles/1"))
            .andExpect(status().isNoContent());

        verify(articleService, times(1)).deleteArticle(1L);
    }

    @Test
    void testDeleteArticleNotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Article not found"))
            .when(articleService).deleteArticle(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/articles/999"))
            .andExpect(status().isNotFound());
    }
}
