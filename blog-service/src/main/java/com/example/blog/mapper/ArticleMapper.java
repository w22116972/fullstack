package com.example.blog.mapper;

import com.example.blog.dto.ArticleDto;
import com.example.blog.dto.ArticleSummaryDto;
import com.example.blog.model.Article;

/**
 * Mapper interface for converting Article entities to DTOs.
 * Centralizes all DTO conversion logic for Article entities.
 */
public interface ArticleMapper {

    /**
     * Converts an Article entity to ArticleDto.
     * Includes all fields: id, title, content, tags, status, timestamps, and author info.
     *
     * @param article the Article entity to convert
     * @return the corresponding ArticleDto
     */
    ArticleDto toDto(Article article);

    /**
     * Converts an Article entity to ArticleSummaryDto.
     * Includes only essential fields for summary views: id, title, tags, status, timestamps, and author info.
     *
     * @param article the Article entity to convert
     * @return the corresponding ArticleSummaryDto
     */
    ArticleSummaryDto toSummaryDto(Article article);
}
