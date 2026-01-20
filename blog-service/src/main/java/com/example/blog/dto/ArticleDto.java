package com.example.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.Instant;

@Data
public class ArticleDto {
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 20000, message = "Content must be between 1 and 20000 characters")
    private String content;

    @Size(max = 100, message = "Tags must be less than 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9,\\s-]*$", message = "Tags can only contain letters, numbers, commas, spaces, and hyphens")
    private String tags;

    @NotBlank(message = "Publish status is required")
    @Pattern(regexp = "^(draft|published)$", message = "Publish status must be 'draft' or 'published'")
    private String publishStatus;

    private Long authorId;
    private String authorEmail;
    private Instant createdAt;
    private Instant updatedAt;
}
