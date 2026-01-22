package com.example.blog.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis cache configuration.
 *
 * Enables caching with graceful error handling when Redis is unavailable.
 * If Redis connection fails, cache operations are skipped without errors.
 * Uses JSON serialization (Jackson) instead of JDK serialization for better
 * compatibility and security.
 * Also provides RedisTemplate for accessing shared auth-cache to check token JTI blacklist.
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

        /**
         * Configures Redis cache to use JSON serialization with Jackson.
         * This avoids the need for DTOs to implement Serializable.
         */
        @Bean
        public RedisCacheConfiguration cacheConfiguration() {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                objectMapper.activateDefaultTyping(
                        LaissezFaireSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY
                );

                return RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofHours(1))
                        .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)))
                        .disableCachingNullValues();
        }
        
        /**
         * Provides a RedisTemplate bean for string key-value operations.
         * Used to check token JTI blacklist from auth-cache microservice.
         *
         * @param connectionFactory the Redis connection factory
         * @return configured RedisTemplate for string operations
         */
        @Bean
        public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
                RedisTemplate<String, String> template = new RedisTemplate<>();
                
                // Set connection factory
                template.setConnectionFactory(connectionFactory);
                
                // Use StringRedisSerializer for both keys and values
                StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
                
                // Set string serialization for keys
                template.setKeySerializer(stringRedisSerializer);
                template.setHashKeySerializer(stringRedisSerializer);
                
                // Set string serialization for values
                template.setValueSerializer(stringRedisSerializer);
                template.setHashValueSerializer(stringRedisSerializer);
                
                // Enable transaction support
                template.setEnableTransactionSupport(true);
                
                template.afterPropertiesSet();
                
                return template;
        }
        
        @Bean
        @Override
        public KeyGenerator keyGenerator() {
                return (target, method, params) -> {
                        StringBuilder sb = new StringBuilder();
                        sb.append(target.getClass().getSimpleName()).append(":");
                        sb.append(method.getName()).append(":");
                        
                        for (Object param : params) {
                                if (param != null) {
                                sb.append(param.toString()).append(":");
                                }
                        }
                        
                        String key = sb.toString();
                        return key.endsWith(":") ? key.substring(0, key.length() - 1) : key;
                };
        }

        @Override
        public CacheErrorHandler errorHandler() {
                return new CacheErrorHandler() {
                @Override
                public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                        log.warn("Cache get error for key '{}' in cache '{}': {}", key, cache.getName(), exception.getMessage());
                }

                @Override
                public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                        log.warn("Cache put error for key '{}' in cache '{}': {}", key, cache.getName(), exception.getMessage());
                }

                @Override
                public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                        log.warn("Cache evict error for key '{}' in cache '{}': {}", key, cache.getName(), exception.getMessage());
                }

                @Override
                public void handleCacheClearError(RuntimeException exception, Cache cache) {
                        log.warn("Cache clear error for cache '{}': {}", cache.getName(), exception.getMessage());
                }
                };
        }
}
