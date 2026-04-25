package egovframework.example.chatbot.config;

import egovframework.example.chatbot.llm.ChatbotLlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
@EnableConfigurationProperties(ChatbotLlmProperties.class)
public class ChatbotCacheConfig extends CachingConfigurerSupport {

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }

    private static class LoggingCacheErrorHandler implements CacheErrorHandler {

        private static final Logger log = LoggerFactory.getLogger(LoggingCacheErrorHandler.class);

        @Override
        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
            log.warn("Cache get failed. cache={}, key={}, error={}", cache.getName(), key, exception.getMessage());
        }

        @Override
        public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
            log.warn("Cache put failed. cache={}, key={}, error={}", cache.getName(), key, exception.getMessage());
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
            log.warn("Cache evict failed. cache={}, key={}, error={}", cache.getName(), key, exception.getMessage());
        }

        @Override
        public void handleCacheClearError(RuntimeException exception, Cache cache) {
            log.warn("Cache clear failed. cache={}, error={}", cache.getName(), exception.getMessage());
        }
    }
}
