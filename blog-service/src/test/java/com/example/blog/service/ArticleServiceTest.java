package com.example.blog.service;

import com.example.blog.dto.ArticleDto;
import com.example.blog.dto.ArticleSummaryDto;
import com.example.blog.exception.ResourceNotFoundException;
import com.example.blog.mapper.ArticleMapper;
import com.example.blog.model.Article;
import com.example.blog.model.Role;
import com.example.blog.model.User;
import com.example.blog.repository.ArticleRepository;
import com.example.blog.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ArticleMapper articleMapper;

    @InjectMocks
    private ArticleService articleService;

    private User testUser;
    private User adminUser;
    private Article testArticle;
    private ArticleDto testArticleDto;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("user@example.com");
        testUser.setRole(Role.USER);

        // Setup admin user
        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(Role.ADMIN);

        // Setup test article
        testArticle = new Article();
        testArticle.setId(1L);
        testArticle.setTitle("Test Article");
        testArticle.setContent("Test Content");
        testArticle.setAuthor(testUser);
        testArticle.setCreatedAt(Instant.now());

        // Setup test article DTO
        testArticleDto = new ArticleDto();
        testArticleDto.setId(1L);
        testArticleDto.setTitle("Test Article");
        testArticleDto.setContent("Test Content");
    }

    private void mockSecurityContext(User user) {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(user.getEmail());

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testGetAllArticlesForAdmin() {
        // Arrange
        mockSecurityContext(adminUser);
        Page<Article> articlePage = new PageImpl<>(List.of(testArticle));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(articleRepository.findAll(any(Pageable.class))).thenReturn(articlePage);
        when(articleMapper.toSummaryDto(testArticle)).thenReturn(new ArticleSummaryDto());

        // Act
        Page<ArticleSummaryDto> result = articleService.getAllArticles(null, mock(Pageable.class));

        // Assert
        assertNotNull(result);
        verify(articleRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void testGetAllArticlesForUserFiltersOwnArticles() {
        // Arrange
        mockSecurityContext(testUser);
        Page<Article> articlePage = new PageImpl<>(List.of(testArticle));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(articleRepository.findByAuthor(eq(testUser), any(Pageable.class))).thenReturn(articlePage);
        when(articleMapper.toSummaryDto(testArticle)).thenReturn(new ArticleSummaryDto());

        // Act
        Page<ArticleSummaryDto> result = articleService.getAllArticles(null, mock(Pageable.class));

        // Assert
        assertNotNull(result);
        verify(articleRepository, times(1)).findByAuthor(eq(testUser), any(Pageable.class));
    }

    @Test
    void testGetAllArticlesWithTitleFilter() {
        // Arrange
        mockSecurityContext(testUser);
        Page<Article> articlePage = new PageImpl<>(List.of(testArticle));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(articleRepository.findByAuthorAndTitleContainingIgnoreCase(
            eq(testUser), eq("Test"), any(Pageable.class))).thenReturn(articlePage);
        when(articleMapper.toSummaryDto(testArticle)).thenReturn(new ArticleSummaryDto());

        // Act
        Page<ArticleSummaryDto> result = articleService.getAllArticles("Test", mock(Pageable.class));

        // Assert
        assertNotNull(result);
        verify(articleRepository, times(1)).findByAuthorAndTitleContainingIgnoreCase(
            eq(testUser), eq("Test"), any(Pageable.class));
    }

    @Test
    void testGetArticleSuccess() {
        // Arrange
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        when(articleMapper.toDto(testArticle)).thenReturn(testArticleDto);

        // Act
        ArticleDto result = articleService.getArticle(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testArticleDto.getId(), result.getId());
        verify(articleRepository, times(1)).findById(1L);
    }

    @Test
    void testGetArticleNotFound() {
        // Arrange
        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> articleService.getArticle(999L));
        verify(articleRepository, times(1)).findById(999L);
    }

    @Test
    void testCreateArticleSuccess() {
        // Arrange
        mockSecurityContext(testUser);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(articleRepository.save(any(Article.class))).thenReturn(testArticle);
        when(articleMapper.toDto(testArticle)).thenReturn(testArticleDto);

        // Act
        ArticleDto result = articleService.createArticle(testArticleDto);

        // Assert
        assertNotNull(result);
        verify(articleRepository, times(1)).save(any(Article.class));
        verify(articleMapper, times(1)).toDto(testArticle);
    }

    @Test
    void testCreateArticleSetsAuthorCorrectly() {
        // Arrange
        mockSecurityContext(testUser);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article article = invocation.getArgument(0);
            assertEquals(testUser, article.getAuthor());
            return article;
        });
        when(articleMapper.toDto(any())).thenReturn(testArticleDto);

        // Act
        articleService.createArticle(testArticleDto);

        // Assert
        verify(articleRepository, times(1)).save(any(Article.class));
    }

    @Test
    void testUpdateArticleSuccess() {
        // Arrange
        mockSecurityContext(testUser);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        when(articleRepository.save(any(Article.class))).thenReturn(testArticle);
        when(articleMapper.toDto(testArticle)).thenReturn(testArticleDto);

        ArticleDto updateDto = new ArticleDto();
        updateDto.setTitle("Updated Title");
        updateDto.setContent("Updated Content");

        // Act
        ArticleDto result = articleService.updateArticle(1L, updateDto);

        // Assert
        assertNotNull(result);
        verify(articleRepository, times(1)).save(any(Article.class));
    }

    @Test
    void testUpdateArticleNotFound() {
        // Arrange
        mockSecurityContext(testUser);
        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
            () -> articleService.updateArticle(999L, testArticleDto));
    }

    @Test
    void testDeleteArticleSuccess() {
        // Arrange
        mockSecurityContext(testUser);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));

        // Act
        articleService.deleteArticle(1L);

        // Assert
        verify(articleRepository, times(1)).delete(testArticle);
    }

    @Test
    void testDeleteArticleNotFound() {
        // Arrange
        mockSecurityContext(testUser);
        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        articleService.deleteArticle(999L);

        // Assert
        verify(articleRepository, never()).delete(any());
    }
}
