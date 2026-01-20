package com.example.blog.mapper;

import com.example.blog.dto.ArticleDto;
import com.example.blog.dto.ArticleSummaryDto;
import com.example.blog.model.Article;
import org.springframework.stereotype.Component;

/**
 * Implementation of ArticleMapper.
 * Handles the conversion logic between Article entities and their DTOs.
 */
@Component
public class ArticleMapperImpl implements ArticleMapper {

    /**
     * Converts an Article entity to ArticleDto.
     * Maps all article fields including author information.
     *
     * @param article the Article entity to convert
     * @return the corresponding ArticleDto with all fields populated
     */
    @Override
    public ArticleDto toDto(Article article) {
        ArticleDto dto = new ArticleDto();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setContent(article.getContent());
        dto.setTags(article.getTags());
        dto.setPublishStatus(article.getPublishStatus());
        dto.setCreatedAt(article.getCreatedAt());
        dto.setUpdatedAt(article.getUpdatedAt());
        if (article.getAuthor() != null) {
            dto.setAuthorId(article.getAuthor().getId());
            dto.setAuthorEmail(article.getAuthor().getEmail());
        }
        return dto;
    }

    /**
     * Converts an Article entity to ArticleSummaryDto.
     * Maps essential fields only, excluding content for list views.
     *
     * @param article the Article entity to convert
     * @return the corresponding ArticleSummaryDto with summary fields populated
     */
    @Override
    public ArticleSummaryDto toSummaryDto(Article article) {
        ArticleSummaryDto dto = new ArticleSummaryDto();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setTags(article.getTags());
        dto.setPublishStatus(article.getPublishStatus());
        dto.setCreatedAt(article.getCreatedAt());
        dto.setUpdatedAt(article.getUpdatedAt());
        if (article.getAuthor() != null) {
            dto.setAuthorId(article.getAuthor().getId());
            dto.setAuthorEmail(article.getAuthor().getEmail());
        }
        return dto;
    }
}
