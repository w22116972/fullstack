package com.example.auth.config;

import com.example.auth.model.Role;
import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * AdminInitializer is responsible for ensuring that a default administrator account exists
 * when the authentication service starts. On application startup, it checks if an admin user
 * is present in the system. If not, it creates one using the credentials specified in the
 * application configuration. This mechanism guarantees that there is always at least one
 * administrator account available for managing the authentication service, which is essential
 * for initial access and ongoing administration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@example.com}")
    private String adminEmail;

    @Value("${admin.password:password123}")
    private String adminPassword;

    @Override
    /**
     * This method is executed on application startup. It checks if an admin user
     * with the configured email exists in the database. If not, it creates a new
     * admin user with the specified email and password, ensuring that an administrator
     * account is always available for system management.
     */
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            log.info("Admin account created: {}", adminEmail);
        } else {
            log.info("Admin account already exists: {}", adminEmail);
        }
    }
}
