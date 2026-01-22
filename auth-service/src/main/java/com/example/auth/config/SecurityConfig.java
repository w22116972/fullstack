package com.example.auth.config;

import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig sets up Spring Security for the authentication service.
 * <p>
 * This configuration class defines beans for password encoding, user details retrieval,
 * authentication management, and HTTP security rules. It ensures that authentication endpoints
 * are publicly accessible, configures stateless session management for JWT-based authentication,
 * and integrates with the application's user repository for user lookup.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;

    /**
     * Configures HTTP security for the authentication service.
     * <p>
     * - Disables CSRF protection (since JWT is used and sessions are stateless).
     * - Sets session management to stateless to support JWT authentication.
     * - Allows public access to login, register, token validation, and actuator endpoints.
     * - Requires authentication for all other endpoints.
     *
     * @param http the HttpSecurity object to configure
     * @return the configured SecurityFilterChain bean
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login", "/auth/register", "/auth/validate", "/actuator/**").permitAll()
                .anyRequest().authenticated()
            );
        
        return http.build();
    }

    /**
     * Defines the password encoder bean using the BCrypt algorithm.
     * <p>
     * BCrypt is a strong hashing algorithm recommended for storing user passwords securely.
     *
     * @return a PasswordEncoder bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Provides a UserDetailsService bean for loading users by email.
     * <p>
     * This service is used by Spring Security to retrieve user details during authentication.
     * It looks up users in the UserRepository by email and throws an exception if not found.
     *
     * @return a UserDetailsService bean
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
            return user;
        };
    }

    /**
     * Provides an AuthenticationManager bean for handling authentication requests.
     * <p>
     * The AuthenticationManager is a central component in Spring Security that processes
     * authentication attempts. It is auto-configured using the provided AuthenticationConfiguration.
     *
     * @param config the AuthenticationConfiguration bean
     * @return the AuthenticationManager bean
     * @throws Exception if retrieval fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
