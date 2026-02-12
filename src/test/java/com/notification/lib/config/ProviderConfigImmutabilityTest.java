package com.notification.lib.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for ProviderConfig immutability improvements.
 */
@DisplayName("ProviderConfig - Immutability")
class ProviderConfigImmutabilityTest {

    @Test
    @DisplayName("should return unmodifiable properties map")
    void shouldReturnUnmodifiableProperties() {
        ProviderConfig config = ProviderConfig.builder()
                .apiKey("test-key")
                .properties(Map.of("key1", "value1"))
                .build();

        Map<String, String> properties = config.getProperties();

        assertThatThrownBy(() -> properties.put("hack", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("should allow reading properties from unmodifiable map")
    void shouldAllowReadingProperties() {
        ProviderConfig config = ProviderConfig.builder()
                .apiKey("test-key")
                .properties(Map.of("projectId", "my-project"))
                .build();

        assertThat(config.getProperties().get("projectId")).isEqualTo("my-project");
    }

    @Test
    @DisplayName("should return empty unmodifiable map when no properties set")
    void shouldReturnEmptyUnmodifiableMapWhenDefault() {
        ProviderConfig config = ProviderConfig.builder()
                .apiKey("test-key")
                .build();

        Map<String, String> properties = config.getProperties();
        assertThat(properties).isEmpty();
        assertThatThrownBy(() -> properties.put("key", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
