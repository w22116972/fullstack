package com.example.blog.security;

import com.example.blog.model.Article;
import com.example.blog.model.User;
import com.example.blog.repository.ArticleRepository;
import com.example.blog.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("articleSecurity")
public class ArticleSecurity {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    public boolean isOwner(Authentication authentication, Long articleId) {
        String userEmail = authentication.getName(); // Get email from authentication object
        Optional<User> userOptional = userRepository.findByEmail(userEmail);

        if (userOptional.isEmpty()) {
            return false;
        }

        User currentUser = userOptional.get();
        Optional<Article> articleOptional = articleRepository.findById(articleId);

        if (articleOptional.isEmpty()) {
            return false;
        }

        Article article = articleOptional.get();
        // Check if the current user is the author of the article or an admin
        return article.getAuthor().getId().equals(currentUser.getId()) || currentUser.getRole().equals(com.example.blog.model.Role.ADMIN);
    }
}
