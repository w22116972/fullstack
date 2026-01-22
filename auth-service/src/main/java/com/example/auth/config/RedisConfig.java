package com.example.auth.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis configuration for token blacklist management.
 * <p>
 * Configures RedisTemplate for storing blacklisted JWT tokens during logout.
 * Tokens are automatically expired from Redis after their JWT expiration time.
 * <p>
 * Uses lazy initialization to allow auth-service to start without Redis availability.
 * This provides graceful degradation - the service works without token caching if Redis is unavailable.
 * <p>
 * Note: This configuration excludes Spring Boot's RedisAutoConfiguration to prevent
 * eager connection attempts at startup. Both RedisConnectionFactory and RedisTemplate
 * are lazily initialized.
 */
@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.timeout:2000}")
    private int redisTimeout;

    /**
     * Creates a lazy RedisConnectionFactory using Lettuce client.
     * <p>
     * This bean is lazily initialized to allow the auth-service to start
     * even when Redis is not available. The connection will only be
     * attempted when first accessed.
     *
     * @return LettuceConnectionFactory configured for the Redis server
     */
    @Bean
    @Lazy
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Initializing RedisConnectionFactory (lazy) - host: {}, port: {}", redisHost, redisPort);

        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }

        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofMillis(redisTimeout))
                .build();

        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .build();

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(redisTimeout))
                .clientOptions(clientOptions)
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
        factory.afterPropertiesSet();

        log.info("RedisConnectionFactory initialized successfully");
        return factory;
    }

    /**
     * Configures RedisTemplate for string key-value operations with lazy initialization.
     * <p>
     * Uses StringRedisSerializer for both keys and values to ensure
     * proper serialization of token blacklist data.
     * <p>
     * Lazy initialization allows auth-service to start even if Redis is not available.
     * The connection will be established when first accessed.
     *
     * @param connectionFactory the Redis connection factory (also lazy)
     * @return configured RedisTemplate
     */
    @Bean
    @Lazy
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("Initializing RedisTemplate (lazy initialization)");
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use string serialization for both keys and values
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        log.info("RedisTemplate initialized successfully");
        return template;
    }
}
