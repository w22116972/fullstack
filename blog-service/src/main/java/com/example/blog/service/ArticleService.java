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

/**
 * ArticleService provides business logic for managing articles in the blog system.
 * <p>
 * This service handles CRUD operations for articles, including listing, retrieving,
 * creating, updating, and deleting articles. It enforces security rules for access control,
 * supports caching for performance, and includes retry logic for database operations.
 * Admin users can access all articles, while regular users can only access their own.
 */
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

    /**
     * Retrieves a paginated list of article summaries.
     * <p>
     * - Admin users see all articles, optionally filtered by title.
     * - Regular users see only their own articles, optionally filtered by title.
     * - Supports retry logic for transient SQL errors.
     *
     * @param title optional title filter
     * @param pageable pagination information
     * @return a page of ArticleSummaryDto objects
     */
    @PreAuthorize("isAuthenticated()")
    /**
     * Retries on transient exceptions:
     * - SQLException: covers most database connectivity and transient errors.
     * - TransientDataAccessException: Spring's abstraction for temporary DB issues (e.g., deadlocks).
     * - CannotAcquireLockException: for lock acquisition failures due to DB contention.
     * - ConcurrencyFailureException: for optimistic/pessimistic locking failures.
     * - SocketTimeoutException: for network timeouts to the DB.
     * - QueryTimeoutException: for query execution timeouts.
     */
    @Retryable(
        retryFor = {
            SQLException.class,
            org.springframework.dao.TransientDataAccessException.class,
            org.springframework.dao.CannotAcquireLockException.class,
            org.springframework.dao.ConcurrencyFailureException.class,
            java.net.SocketTimeoutException.class,
            org.springframework.dao.QueryTimeoutException.class
        },
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
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

    /**
     * Retrieves a single article by its ID.
     * <p>
     * - Only admin users or the article's owner can access the article.
     * - Uses caching to improve performance for repeated access.
     * - Supports retry logic for transient SQL errors.
     *
     * @param id the ID of the article
     * @return the ArticleDto for the requested article
     * @throws ResourceNotFoundException if the article does not exist
     */
    @PreAuthorize("hasRole('ADMIN') or @articleSecurity.isOwner(authentication, #id)")
    /**
     * Retries on transient exceptions:
     * - SQLException: covers most database connectivity and transient errors.
     * - TransientDataAccessException: Spring's abstraction for temporary DB issues (e.g., deadlocks).
     * - CannotAcquireLockException: for lock acquisition failures due to DB contention.
     * - ConcurrencyFailureException: for optimistic/pessimistic locking failures.
     * - SocketTimeoutException: for network timeouts to the DB.
     * - QueryTimeoutException: for query execution timeouts.
     */
    @Retryable(
        retryFor = {
            SQLException.class,
            org.springframework.dao.TransientDataAccessException.class,
            org.springframework.dao.CannotAcquireLockException.class,
            org.springframework.dao.ConcurrencyFailureException.class,
            java.net.SocketTimeoutException.class,
            org.springframework.dao.QueryTimeoutException.class
        },
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    @Cacheable(value = "articles", key = "#id")
    public ArticleDto getArticle(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + id));
        // The permission check is now handled by @PreAuthorize
        return articleMapper.toDto(article);
    }

    /**
     * Creates a new article.
     * <p>
     * - Any authenticated user can create an article.
     * - Sets the current user as the author.
     * - Logs creation events for auditing.
     *
     * @param dto the ArticleDto containing article data
     * @return the created ArticleDto
     */
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

    /**
     * Updates an existing article.
     * <p>
     * - Only admin users or the article's owner can update the article.
     * - Updates title, content, tags, and publish status.
     * - Logs update events for auditing.
     *
     * @param id the ID of the article to update
     * @param dto the ArticleDto containing updated data
     * @return the updated ArticleDto
     * @throws ResourceNotFoundException if the article does not exist
     */
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

    /**
     * Deletes an article by its ID.
     * <p>
     * - Only admin users or the article's owner can delete the article.
     * - Evicts the article from cache after deletion.
     * - Logs deletion events for auditing.
     *
     * @param id the ID of the article to delete
     */
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

    /**
     * Retrieves the current authenticated user from the security context.
     * <p>
     * - Used internally for associating articles with users and filtering results.
     * - Handles cases where principal may be a User or UserDetails.
     *
     * @return the current User
     * @throws ResourceNotFoundException if the user cannot be found
     */
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

    /**
     * Checks if the given user has the ADMIN role.
     * <p>
     * - Used internally for access control logic in article listing.
     *
     * @param user the User to check
     * @return true if the user is an admin, false otherwise
     */
    private boolean isAdmin(User user) {
        return Role.ADMIN.equals(user.getRole());
    }
}