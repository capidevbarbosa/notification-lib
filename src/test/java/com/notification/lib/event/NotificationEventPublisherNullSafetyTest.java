package com.notification.lib.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests for null-safety improvements in NotificationEventPublisher.
 */
@DisplayName("NotificationEventPublisher - Null Safety")
class NotificationEventPublisherNullSafetyTest {

    @Test
    @DisplayName("should handle null initial listeners list without NPE")
    void shouldHandleNullInitialListeners() {
        assertThatCode(() -> new NotificationEventPublisher(null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should create publisher with empty listeners when null is passed")
    void shouldCreateWithEmptyListenersWhenNull() {
        NotificationEventPublisher publisher = new NotificationEventPublisher(null);
        assertThat(publisher.getListenerCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("should work normally after null initialization")
    void shouldWorkAfterNullInitialization() {
        NotificationEventPublisher publisher = new NotificationEventPublisher(null);
        publisher.addListener(event -> {});
        assertThat(publisher.getListenerCount()).isEqualTo(1);
    }
}
