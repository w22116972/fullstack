package com.example.blog.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.blog.dto.ArticleDto;
import com.example.blog.dto.ArticleSummaryDto;
import com.example.blog.service.ArticleService;

/**
 * REST API controller for managing articles.
 * 
 * Provides endpoints for CRUD operations on articles including:
 * - Retrieving all articles with pagination and filtering
 * - Fetching a single article by ID
 * - Creating new articles
 * - Updating existing articles
 * - Deleting articles
 * 
 * Authorization is handled at the service layer using @PreAuthorize annotations.
 * Admins can access all articles, while regular users can only access their own.
 * 
 * @see com.example.backend.service.ArticleService
 */
@Validated
@RestController
@RequestMapping("/articles")
@CrossOrigin(origins = "${cors.allowed-origins:http://localhost:8080}", allowCredentials = "true")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @org.springframework.beans.factory.annotation.Value("${app.default-page-size:10}")
    private int defaultPageSize;

    /**
     * Retrieves a paginated list of articles.
     * 
     * Admins see all articles. Regular users see only their own articles.
     * Results can be filtered by article title (case-insensitive).
     * 
     * Validates pagination parameters to prevent invalid requests:
     * - Page number must be non-negative
     * - Page size must be between 1 and 100
     * 
     * @param title optional filter to search articles by title
     * @param pageable pagination information (page number, size, sorting)
     * @return a paginated list of article summaries
     * @throws ResponseStatusException with BAD_REQUEST (400) if page or size parameters are invalid
     */
    @GetMapping
    public ResponseEntity<Page<ArticleSummaryDto>> getAllArticles(
            @RequestParam(required = false) String title,
            @PageableDefault Pageable pageable) {
        // Defensive validation for pageable
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page number must be non-negative");
        }
        if (size <= 0 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be between 1 and 100");
        }
        Pageable effectivePageable = pageable;
        if (size == Integer.MAX_VALUE) {
            effectivePageable = org.springframework.data.domain.PageRequest.of(page, defaultPageSize, pageable.getSort());
        }
        return ResponseEntity.ok(articleService.getAllArticles(title, effectivePageable));
    }

    /**
     * Retrieves a single article by its ID.
     * 
     * Admins can retrieve any article. Regular users can only retrieve their own articles.
     * Authorization is enforced at the service layer.
     * 
     * @param id the article ID (must be a positive integer)
     * @return the full article details
     * @throws ResponseStatusException with BAD_REQUEST (400) if id is null or not positive
     * @throws ResourceNotFoundException with NOT_FOUND (404) if the article does not exist
     * @throws AccessDeniedException with FORBIDDEN (403) if the user lacks permission to access this article
     */
    @GetMapping("/{id}")
    public ResponseEntity<ArticleDto> getArticle(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Article id must be positive");
        }
        return ResponseEntity.ok(articleService.getArticle(id));
    }

    /**
     * Creates a new article.
     * 
     * Only authenticated users can create articles. The author is automatically set to the current user.
     * The article content is validated before creation:
     * - Title is required and must not be empty
     * - Content must be provided
     * 
     * @param dto the article data transfer object containing article details
     * @return the created article with generated ID and timestamps
     * @throws ResponseStatusException with BAD_REQUEST (400) if validation fails
     * @throws AccessDeniedException with FORBIDDEN (403) if user is not authenticated
     */
    @PostMapping
    public ResponseEntity<ArticleDto> createArticle(@Valid @RequestBody ArticleDto dto) {
        return ResponseEntity.ok(articleService.createArticle(dto));
    }

    /**
     * Updates an existing article.
     * 
     * Admins can update any article. Regular users can only update their own articles.
     * All article fields can be updated except the ID and author.
     * 
     * @param id the article ID to update (must be a positive integer)
     * @param dto the updated article data
     * @return the updated article details
     * @throws ResponseStatusException with BAD_REQUEST (400) if id is null or not positive
     * @throws ResourceNotFoundException with NOT_FOUND (404) if the article does not exist
     * @throws AccessDeniedException with FORBIDDEN (403) if the user lacks permission to update this article
     * @throws ResponseStatusException with BAD_REQUEST (400) if validation of the new data fails
     */
    @PutMapping("/{id}")
    public ResponseEntity<ArticleDto> updateArticle(@PathVariable Long id, @Valid @RequestBody ArticleDto dto) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Article id must be positive");
        }
        return ResponseEntity.ok(articleService.updateArticle(id, dto));
    }

    /**
     * Deletes an article by its ID.
     * 
     * Admins can delete any article. Regular users can only delete their own articles.
     * Deletion is permanent and cannot be reversed.
     * 
     * @param id the article ID to delete (must be a positive integer)
     * @return HTTP 204 No Content on successful deletion
     * @throws ResponseStatusException with BAD_REQUEST (400) if id is null or not positive
     * @throws ResourceNotFoundException with NOT_FOUND (404) if the article does not exist
     * @throws AccessDeniedException with FORBIDDEN (403) if the user lacks permission to delete this article
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Article id must be positive");
        }
        articleService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }
}
