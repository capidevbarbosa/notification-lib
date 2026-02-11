package com.notification.lib.core;

/**
 * Core abstraction for all notification channels.
 * Implements the Strategy pattern - each channel (Email, SMS, Push)
 * provides its own implementation of send logic.
 *
 * <p>Uses generics to ensure type safety: each channel works with
 * its specific message type (e.g., EmailChannel works with EmailMessage).</p>
 *
 * <p><b>Open/Closed Principle:</b> New channels can be added by implementing
 * this interface without modifying existing code.</p>
 *
 * @param <T> the specific message type this channel handles
 */
public interface NotificationChannel<T extends ChannelMessage> {

    /**
     * Sends a notification through this channel.
     *
     * @param message the channel-specific message to send
     * @return the result of the send operation
     * @throws com.notification.lib.exception.ValidationException if the message is invalid
     * @throws com.notification.lib.exception.SendException if the send operation fails
     */
    NotificationResult send(T message);

    /**
     * Returns the channel type this implementation handles.
     *
     * @return the channel type
     */
    ChannelType getChannelType();

    /**
     * Returns the name of the current provider being used by this channel.
     *
     * @return the provider name (e.g., "SendGrid", "Twilio")
     */
    String getProviderName();
}
