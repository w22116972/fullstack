package com.example.blog.repository;

import com.example.blog.model.Article;
import com.example.blog.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    // Fetch author with article to avoid N+1 queries
    @EntityGraph(attributePaths = {"author"})
    Page<Article> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Page<Article> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Page<Article> findByAuthor(User author, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Page<Article> findByAuthorAndTitleContainingIgnoreCase(User author, String title, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Optional<Article> findById(Long id);

    // Custom query with JOIN FETCH for complex queries
    @Query("SELECT a FROM Article a JOIN FETCH a.author WHERE a.id = :id")
    Optional<Article> findByIdWithAuthor(@Param("id") Long id);
}
