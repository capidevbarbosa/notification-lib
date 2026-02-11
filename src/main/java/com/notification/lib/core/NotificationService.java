package com.notification.lib.core;

import com.notification.lib.config.NotificationConfig;
import com.notification.lib.event.NotificationEvent;
import com.notification.lib.event.NotificationEventPublisher;
import com.notification.lib.exception.NotificationException;
import com.notification.lib.exception.SendException;
import com.notification.lib.exception.ValidationException;
import com.notification.lib.retry.RetryExecutor;
import com.notification.lib.retry.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main facade for sending notifications through the library.
 * Orchestrates validation, channel routing, retry logic, and event publishing.
 *
 * <p><b>Facade Pattern:</b> Provides a simple unified API for the complex
 * subsystem of channels, providers, validators, retry, and events.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * NotificationService service = new NotificationService(config);
 *
 * NotificationResult result = service.send(EmailMessage.builder()
 *     .recipient("user@example.com")
 *     .from("noreply@company.com")
 *     .subject("Welcome!")
 *     .body("Hello, welcome aboard!")
 *     .build());
 * </pre>
 */
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationConfig config;
    private final NotificationEventPublisher eventPublisher;

    public NotificationService(NotificationConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("NotificationConfig must not be null");
        }
        this.config = config;
        this.eventPublisher = new NotificationEventPublisher(config.getListeners());
    }

    /**
     * Sends a notification through the appropriate channel.
     * The channel is determined by the message's {@link ChannelType}.
     *
     * <p>Flow: Route to channel -> Publish PENDING -> Send (with optional retry) -> Publish SENT/FAILED -> Return result</p>
     *
     * @param message the channel-specific message to send
     * @return the result of the send operation
     * @throws ValidationException if the message fails validation
     * @throws SendException if the send operation fails after all retries
     * @throws NotificationException if no channel is registered for the message type
     */
    public <T extends ChannelMessage> NotificationResult send(T message) {
        if (message == null) {
            throw new ValidationException("Message must not be null");
        }

        ChannelType channelType = message.getChannelType();
        String messageId = message.getId();

        log.info("Sending {} notification [{}]", channelType, messageId);

        // Resolve the channel for this message type
        NotificationChannel<T> channel = config.<T>getChannel(channelType)
                .orElseThrow(() -> new NotificationException(
                        "No channel registered for type: " + channelType +
                                ". Registered channels: " + config.getRegisteredChannels()));

        // Publish PENDING event
        eventPublisher.publish(NotificationEvent.pending(messageId, channelType));

        try {
            // Publish SENDING event
            eventPublisher.publish(NotificationEvent.sending(messageId, channelType));

            NotificationResult result;

            // Execute with retry if configured
            if (config.hasRetryPolicy()) {
                RetryPolicy retryPolicy = config.getRetryPolicy();
                RetryExecutor retryExecutor = new RetryExecutor(retryPolicy,
                        (attempt, maxRetries) -> eventPublisher.publish(
                                NotificationEvent.retrying(messageId, channelType, attempt)));

                result = retryExecutor.executeWithRetry(() -> channel.send(message));
            } else {
                result = channel.send(message);
            }

            // Publish SENT event on success
            if (result.isSuccess()) {
                eventPublisher.publish(NotificationEvent.sent(messageId, channelType, result));
                log.info("{} notification [{}] sent successfully via {}", channelType, messageId, result.getProviderName());
            } else {
                eventPublisher.publish(NotificationEvent.failed(messageId, channelType,
                        result.getError() != null ? result.getError().getMessage() : "Unknown error"));
                log.warn("{} notification [{}] failed: {}", channelType, messageId,
                        result.getError() != null ? result.getError().getMessage() : "Unknown error");
            }

            return result;

        } catch (ValidationException e) {
            // Validation errors are NOT retried
            eventPublisher.publish(NotificationEvent.failed(messageId, channelType, e.getMessage()));
            log.error("{} notification [{}] validation failed: {}", channelType, messageId, e.getMessage());
            throw e;

        } catch (SendException e) {
            eventPublisher.publish(NotificationEvent.failed(messageId, channelType, e.getMessage()));
            log.error("{} notification [{}] send failed: {}", channelType, messageId, e.getMessage());
            throw e;

        } catch (Exception e) {
            eventPublisher.publish(NotificationEvent.failed(messageId, channelType, e.getMessage()));
            log.error("{} notification [{}] unexpected error: {}", channelType, messageId, e.getMessage(), e);
            throw new NotificationException("Unexpected error sending notification: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the event publisher for runtime listener management.
     */
    public NotificationEventPublisher getEventPublisher() {
        return eventPublisher;
    }
}
