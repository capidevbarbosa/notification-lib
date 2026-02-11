package com.notification.lib.core;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for all channel-specific messages.
 * Each notification channel (Email, SMS, Push) extends this class
 * to add its own specific fields while maintaining a common contract.
 *
 * Uses Lombok's @SuperBuilder to allow subclasses to inherit the builder pattern.
 */
@Getter
@SuperBuilder
public abstract class ChannelMessage {

    /**
     * Unique identifier for this message, auto-generated if not provided.
     */
    private final String id;

    /**
     * The recipient of the notification (email, phone number, device token, etc.)
     */
    private final String recipient;

    /**
     * The main body/content of the notification.
     */
    private final String body;

    /**
     * Additional metadata that can be attached to any message.
     * Useful for tracking, correlation IDs, custom headers, etc.
     */
    private final Map<String, String> metadata;

    /**
     * Timestamp when the message was created.
     */
    private final Instant createdAt;

    /**
     * Returns the channel type this message belongs to.
     * Each subclass must implement this to identify its channel.
     */
    public abstract ChannelType getChannelType();

    /**
     * Custom builder base class to set default values.
     */
    public abstract static class ChannelMessageBuilder<C extends ChannelMessage, B extends ChannelMessageBuilder<C, B>> {

        protected ChannelMessageBuilder() {
            this.id = UUID.randomUUID().toString();
            this.metadata = new HashMap<>();
            this.createdAt = Instant.now();
        }
    }
}
