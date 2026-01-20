package com.example.blog.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class ArticleSummaryDto {
    private Long id;
    private String title;
    private String tags;
    private String publishStatus;
    private Long authorId;
    private String authorEmail;
    private Instant createdAt;
    private Instant updatedAt;
}
