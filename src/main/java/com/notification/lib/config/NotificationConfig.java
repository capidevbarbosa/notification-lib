package com.notification.lib.config;

import com.notification.lib.core.ChannelMessage;
import com.notification.lib.core.ChannelType;
import com.notification.lib.core.NotificationChannel;
import com.notification.lib.event.NotificationListener;
import com.notification.lib.retry.RetryPolicy;

import java.util.*;

/**
 * Central configuration for the notification library.
 * Uses Builder pattern to provide a fluent, type-safe configuration API.
 *
 * <p>All configuration is done through Java code (no YAML/properties files),
 * making it framework-agnostic and IDE-friendly.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * NotificationConfig config = NotificationConfig.builder()
 *     .registerChannel(ChannelType.EMAIL, emailChannel)
 *     .registerChannel(ChannelType.SMS, smsChannel)
 *     .withRetryPolicy(RetryPolicy.builder().maxRetries(3).build())
 *     .build();
 * </pre>
 */
public class NotificationConfig {

    private final Map<ChannelType, NotificationChannel<? extends ChannelMessage>> channels;
    private final RetryPolicy retryPolicy;
    private final List<NotificationListener> listeners;

    private NotificationConfig(Builder builder) {
        this.channels = Collections.unmodifiableMap(builder.channels);
        this.retryPolicy = builder.retryPolicy;
        this.listeners = Collections.unmodifiableList(builder.listeners);
    }

    /**
     * Gets the registered channel for the given type.
     *
     * @return Optional containing the channel, or empty if not registered
     */
    @SuppressWarnings("unchecked")
    public <T extends ChannelMessage> Optional<NotificationChannel<T>> getChannel(ChannelType type) {
        return Optional.ofNullable((NotificationChannel<T>) channels.get(type));
    }

    /**
     * Returns all registered channel types.
     */
    public Set<ChannelType> getRegisteredChannels() {
        return channels.keySet();
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public List<NotificationListener> getListeners() {
        return listeners;
    }

    public boolean hasRetryPolicy() {
        return retryPolicy != null;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for NotificationConfig.
     */
    public static class Builder {
        private final Map<ChannelType, NotificationChannel<? extends ChannelMessage>> channels = new HashMap<>();
        private RetryPolicy retryPolicy;
        private final List<NotificationListener> listeners = new ArrayList<>();

        public <T extends ChannelMessage> Builder registerChannel(ChannelType type, NotificationChannel<T> channel) {
            if (type == null) throw new IllegalArgumentException("ChannelType must not be null");
            if (channel == null) throw new IllegalArgumentException("NotificationChannel must not be null");
            channels.put(type, channel);
            return this;
        }

        public Builder withRetryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public Builder addListener(NotificationListener listener) {
            if (listener == null) throw new IllegalArgumentException("NotificationListener must not be null");
            listeners.add(listener);
            return this;
        }

        public NotificationConfig build() {
            if (channels.isEmpty()) {
                throw new IllegalStateException("At least one notification channel must be registered");
            }
            return new NotificationConfig(this);
        }
    }
}
