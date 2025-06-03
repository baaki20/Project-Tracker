package com.buildmaster.projecttracker.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    // Simple cache configuration is handled by application.properties
    // For production, consider using Redis or Hazelcast
}