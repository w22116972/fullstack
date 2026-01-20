package com.example.blog.config;

import lombok.extern.slf4j.Slf4j;


import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;

import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

/**
 * Redis cache configuration.
 *
 * Enables caching with graceful error handling when Redis is unavailable.
 * If Redis connection fails, cache operations are skipped without errors.
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {
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
