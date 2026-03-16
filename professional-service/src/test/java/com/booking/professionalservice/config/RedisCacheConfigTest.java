package com.booking.professionalservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class RedisCacheConfigTest {

    private RedisCacheConfig redisCacheConfig;
    private RedisConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        this.redisCacheConfig = new RedisCacheConfig();
        this.connectionFactory = mock(RedisConnectionFactory.class);
    }

    @Test
    void redisCacheManager_shouldCreateManager() {

        // When
        RedisCacheManager cacheManager = redisCacheConfig.redisCacheManager(connectionFactory);

        // Then
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager.isAllowRuntimeCacheCreation()).isTrue();
        assertThat(cacheManager.getCacheConfigurations()).isEmpty();

        verifyNoInteractions(connectionFactory);

    }

    @Test
    void redisCacheManager_shouldConfigureDefaultCacheSettings() {

        // Given
        RedisCacheManager cacheManager = redisCacheConfig.redisCacheManager(connectionFactory);

        // When
        RedisCacheConfiguration defaultConfig =
                ReflectionTestUtils.invokeMethod(cacheManager, "getDefaultCacheConfiguration");

        // Then
        assertThat(defaultConfig).isNotNull();
        assertThat(defaultConfig.usePrefix()).isTrue();
        assertThat(defaultConfig.getAllowCacheNullValues()).isTrue();
        assertThat(defaultConfig.getKeyPrefixFor("professionals-vehicles"))
                .isEqualTo("professionals-vehicles::");

    }

    @Test
    void redisCacheManager_shouldUseStringSerializerForKeys_andJsonSerializerForValues() {

        // Given
        RedisCacheManager cacheManager = redisCacheConfig.redisCacheManager(connectionFactory);

        RedisCacheConfiguration defaultConfig =
                ReflectionTestUtils.invokeMethod(cacheManager, "getDefaultCacheConfiguration");

        assertThat(defaultConfig).isNotNull();

        RedisSerializationContext.SerializationPair<String> keyPair =
                defaultConfig.getKeySerializationPair();

        RedisSerializationContext.SerializationPair<Object> valuePair =
                defaultConfig.getValueSerializationPair();

        // When - key round trip
        ByteBuffer keyBuffer = keyPair.write("vehicle:1");
        String restoredKey = keyPair.read(keyBuffer.duplicate());

        // Then - String key serializer behavior
        assertThat(restoredKey).isEqualTo("vehicle:1");

        // Given - value round trip
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", 1);
        value.put("name", "Cleaner 1");

        // When
        ByteBuffer valueBuffer = valuePair.write(value);
        Object restoredValue = valuePair.read(valueBuffer.duplicate());

        // Then - JSON serializer behavior
        assertThat(restoredValue).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> restoredMap = (Map<String, Object>) restoredValue;

        assertThat(restoredMap).containsKeys("id", "name");
        assertThat(restoredMap.get("name")).isEqualTo("Cleaner 1");
        assertThat(((Number) restoredMap.get("id")).intValue()).isEqualTo(1);

    }

}