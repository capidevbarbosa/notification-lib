package com.notification.lib.config;

import com.notification.lib.core.ChannelMessage;
import com.notification.lib.core.ChannelType;
import com.notification.lib.core.NotificationChannel;
import com.notification.lib.event.NotificationEventPublisher;
import com.notification.lib.event.NotificationListener;
import com.notification.lib.retry.RetryExecutor;
import com.notification.lib.retry.RetryExecutorFactory;
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
    private final NotificationEventPublisher eventPublisher;
    private final RetryExecutorFactory retryExecutorFactory;

    /** Default factory that creates the standard blocking {@link RetryExecutor}. */
    private static final RetryExecutorFactory DEFAULT_RETRY_FACTORY = RetryExecutor::new;

    private NotificationConfig(Builder builder) {
        this.channels = Collections.unmodifiableMap(builder.channels);
        this.retryPolicy = builder.retryPolicy;
        this.listeners = Collections.unmodifiableList(builder.listeners);
        this.eventPublisher = builder.eventPublisher;
        this.retryExecutorFactory = builder.retryExecutorFactory != null
                ? builder.retryExecutorFactory : DEFAULT_RETRY_FACTORY;
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

    /**
     * Returns the configured event publisher, or creates a default one from the listeners list.
     *
     * <p><b>Dependency Inversion:</b> Allows injecting a custom publisher for testing
     * or alternative event dispatching strategies.</p>
     */
    public NotificationEventPublisher getOrCreateEventPublisher() {
        if (eventPublisher != null) {
            return eventPublisher;
        }
        return new NotificationEventPublisher(listeners);
    }

    /**
     * Returns the configured retry executor factory.
     * Defaults to creating standard blocking {@link RetryExecutor} instances.
     */
    public RetryExecutorFactory getRetryExecutorFactory() {
        return retryExecutorFactory;
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
        private NotificationEventPublisher eventPublisher;
        private RetryExecutorFactory retryExecutorFactory;

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

        /**
         * Sets a custom event publisher. If not set, a default publisher
         * will be created from the registered listeners.
         *
         * @param eventPublisher the custom event publisher
         * @return this builder
         */
        public Builder withEventPublisher(NotificationEventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
            return this;
        }

        /**
         * Sets a custom retry executor factory. If not set, the default
         * blocking {@link RetryExecutor} factory is used.
         *
         * @param retryExecutorFactory the custom factory
         * @return this builder
         */
        public Builder withRetryExecutorFactory(RetryExecutorFactory retryExecutorFactory) {
            this.retryExecutorFactory = retryExecutorFactory;
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
