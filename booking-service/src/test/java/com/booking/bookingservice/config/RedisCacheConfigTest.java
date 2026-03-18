package com.booking.bookingservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.Serializable;
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
        redisCacheConfig = new RedisCacheConfig();
        connectionFactory = mock(RedisConnectionFactory.class);
    }

    @Test
    void redisCacheManager_givenConnectionFactory_thenCreatesRedisCacheManager() {
        // Given / When
        RedisCacheManager cacheManager = redisCacheConfig.redisCacheManager(connectionFactory);

        // Then
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager.isAllowRuntimeCacheCreation()).isTrue();
        assertThat(cacheManager.getCacheConfigurations()).isEmpty();

        verifyNoInteractions(connectionFactory);
    }

    @Test
    void redisCacheManager_givenConnectionFactory_thenUsesExpectedDefaultConfiguration() {
        // Given
        RedisCacheManager cacheManager = redisCacheConfig.redisCacheManager(connectionFactory);

        // When
        RedisCacheConfiguration defaultConfig =
                ReflectionTestUtils.invokeMethod(cacheManager, "getDefaultCacheConfiguration");

        // Then
        assertThat(defaultConfig).isNotNull();
        assertThat(defaultConfig.usePrefix()).isTrue();
        assertThat(defaultConfig.getAllowCacheNullValues()).isFalse();
        assertThat(defaultConfig.getKeyPrefixFor("booking-availability-by-date-v4"))
                .isEqualTo("booking-availability-by-date-v4::");
    }

    @Test
    void redisCacheManager_givenConfiguration_thenUsesStringKeySerializerAndJdkValueSerializer() {
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
        ByteBuffer keyBuffer = keyPair.write("booking:1");
        String restoredKey = keyPair.read(keyBuffer.duplicate());

        // Then
        assertThat(restoredKey).isEqualTo("booking:1");

        // Given - value round trip
        TestSerializableValue value = new TestSerializableValue("veh-1", 2);

        // When
        ByteBuffer valueBuffer = valuePair.write(value);
        Object restoredValue = valuePair.read(valueBuffer.duplicate());

        // Then
        assertThat(restoredValue).isInstanceOf(TestSerializableValue.class);
        assertThat(restoredValue).isEqualTo(value);
    }

    @Test
    void redisCacheManager_givenSerializableMap_thenCanRoundTripValue() {
        // Given
        RedisCacheManager cacheManager = redisCacheConfig.redisCacheManager(connectionFactory);

        RedisCacheConfiguration defaultConfig =
                ReflectionTestUtils.invokeMethod(cacheManager, "getDefaultCacheConfiguration");

        RedisSerializationContext.SerializationPair<Object> valuePair =
                defaultConfig.getValueSerializationPair();

        Map<String, Serializable> value = new LinkedHashMap<>();
        value.put("vehicleId", "veh-1");
        value.put("count", 2);

        // When
        ByteBuffer valueBuffer = valuePair.write(value);
        Object restoredValue = valuePair.read(valueBuffer.duplicate());

        // Then
        assertThat(restoredValue).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> restoredMap = (Map<String, Object>) restoredValue;

        assertThat(restoredMap).containsEntry("vehicleId", "veh-1");
        assertThat(((Number) restoredMap.get("count")).intValue()).isEqualTo(2);
    }

    private static class TestSerializableValue implements Serializable {
        private final String vehicleId;
        private final int professionalCount;

        private TestSerializableValue(String vehicleId, int professionalCount) {
            this.vehicleId = vehicleId;
            this.professionalCount = professionalCount;
        }

        public String getVehicleId() {
            return vehicleId;
        }

        public int getProfessionalCount() {
            return professionalCount;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof TestSerializableValue other)) return false;
            return professionalCount == other.professionalCount
                    && java.util.Objects.equals(vehicleId, other.vehicleId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(vehicleId, professionalCount);
        }
    }

}