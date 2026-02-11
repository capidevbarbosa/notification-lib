package com.notification.lib.event;

import com.notification.lib.core.ChannelType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationEventPublisher")
class NotificationEventPublisherTest {

    @Test
    @DisplayName("should publish events to all listeners")
    void shouldPublishToAllListeners() {
        List<NotificationEvent> events1 = new ArrayList<>();
        List<NotificationEvent> events2 = new ArrayList<>();

        NotificationEventPublisher publisher = new NotificationEventPublisher();
        publisher.addListener(events1::add);
        publisher.addListener(events2::add);

        NotificationEvent event = NotificationEvent.pending("msg-1", ChannelType.EMAIL);
        publisher.publish(event);

        assertThat(events1).hasSize(1);
        assertThat(events2).hasSize(1);
        assertThat(events1.get(0).getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    @DisplayName("should isolate listener failures")
    void shouldIsolateListenerFailures() {
        List<NotificationEvent> events = new ArrayList<>();

        NotificationEventPublisher publisher = new NotificationEventPublisher();
        publisher.addListener(e -> { throw new RuntimeException("Bad listener"); });
        publisher.addListener(events::add);

        NotificationEvent event = NotificationEvent.sent("msg-1", ChannelType.EMAIL, null);
        publisher.publish(event);

        // Second listener should still receive the event
        assertThat(events).hasSize(1);
    }

    @Test
    @DisplayName("should remove listener")
    void shouldRemoveListener() {
        List<NotificationEvent> events = new ArrayList<>();
        NotificationListener listener = events::add;

        NotificationEventPublisher publisher = new NotificationEventPublisher();
        publisher.addListener(listener);
        publisher.removeListener(listener);

        publisher.publish(NotificationEvent.pending("msg-1", ChannelType.EMAIL));

        assertThat(events).isEmpty();
    }

    @Test
    @DisplayName("should track listener count")
    void shouldTrackListenerCount() {
        NotificationEventPublisher publisher = new NotificationEventPublisher();
        assertThat(publisher.getListenerCount()).isEqualTo(0);

        publisher.addListener(e -> {});
        assertThat(publisher.getListenerCount()).isEqualTo(1);

        publisher.addListener(e -> {});
        assertThat(publisher.getListenerCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("should create events with factory methods")
    void shouldCreateEventsWithFactoryMethods() {
        NotificationEvent pending = NotificationEvent.pending("msg-1", ChannelType.EMAIL);
        assertThat(pending.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(pending.getTimestamp()).isNotNull();

        NotificationEvent sending = NotificationEvent.sending("msg-1", ChannelType.SMS);
        assertThat(sending.getStatus()).isEqualTo(NotificationStatus.SENDING);

        NotificationEvent failed = NotificationEvent.failed("msg-1", ChannelType.PUSH, "error");
        assertThat(failed.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(failed.getErrorMessage()).isEqualTo("error");

        NotificationEvent retrying = NotificationEvent.retrying("msg-1", ChannelType.EMAIL, 2);
        assertThat(retrying.getStatus()).isEqualTo(NotificationStatus.RETRYING);
        assertThat(retrying.getRetryAttempt()).isEqualTo(2);
    }
}
