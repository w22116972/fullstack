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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ArticleService {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ArticleMapper articleMapper;

    // Any authenticated user can list articles,
    // with logic inside to filter based on role.
    @PreAuthorize("isAuthenticated()")
    @Retryable(retryFor = {SQLException.class}, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    public Page<ArticleSummaryDto> getAllArticles(String title, Pageable pageable) {
        User currentUser = getCurrentUser();
        boolean isAdmin = isAdmin(currentUser);
        Page<Article> articles;

        if (isAdmin) {
             if (title != null && !title.isEmpty()) {
                articles = articleRepository.findByTitleContainingIgnoreCase(title, pageable);
             } else {
                articles = articleRepository.findAll(pageable);
             }
        } else {
            // Non-admin users only see their own articles
            if (title != null && !title.isEmpty()) {
                articles = articleRepository.findByAuthorAndTitleContainingIgnoreCase(currentUser, title, pageable);
            } else {
                articles = articleRepository.findByAuthor(currentUser, pageable);
            }
        }
        return articles.map(articleMapper::toSummaryDto);
    }

    @PreAuthorize("hasRole('ADMIN') or @articleSecurity.isOwner(authentication, #id)")
    @Retryable(retryFor = {SQLException.class}, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    @Cacheable(value = "articles", key = "#id")
    public ArticleDto getArticle(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + id));
        // The permission check is now handled by @PreAuthorize
        return articleMapper.toDto(article);
    }

    @Transactional
    @PreAuthorize("isAuthenticated()") // Any authenticated user can create an article
    public ArticleDto createArticle(ArticleDto dto) {
        log.info("Creating new article with title: {}", dto.getTitle());
        Article article = new Article();
        article.setTitle(dto.getTitle());
        article.setContent(dto.getContent());
        article.setTags(dto.getTags());
        article.setPublishStatus(dto.getPublishStatus());

        article.setAuthor(getCurrentUser());

        Article saved = articleRepository.save(article);
        log.info("Article created successfully with id: {}", saved.getId());
        return articleMapper.toDto(saved);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @articleSecurity.isOwner(authentication, #id)")
    public ArticleDto updateArticle(Long id, ArticleDto dto) {
        log.info("Updating article with id: {}", id);
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + id));

        // Ownership check is now handled by @PreAuthorize
        article.setTitle(dto.getTitle());
        article.setContent(dto.getContent());
        article.setTags(dto.getTags());
        article.setPublishStatus(dto.getPublishStatus());

        Article saved = articleRepository.save(article);
        return articleMapper.toDto(saved);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @articleSecurity.isOwner(authentication, #id)")
    @CacheEvict(value = "articles", key = "#id")
    public void deleteArticle(Long id) {
        log.info("Deleting article with id: {}", id);
        Article article = articleRepository.findById(id).orElse(null);

        if (article == null) {
            log.info("Article with id {} already deleted or does not exist", id);
            return;
        }

        // Ownership check is now handled by @PreAuthorize
        articleRepository.delete(article);
    }

    // Retained for internal logic of getAllArticles and createArticle
    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        // Principal is the User object set by JwtAuthFilter
        if (principal instanceof User) {
            return (User) principal;
        }
        
        // Fallback: if for some reason principal is UserDetails, get username and query DB
        String email = principal.toString();
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // Retained for internal logic of getAllArticles
    private boolean isAdmin(User user) {
        return Role.ADMIN.equals(user.getRole());
    }
}