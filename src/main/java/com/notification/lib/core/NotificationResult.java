package com.notification.lib.core;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Represents the result of a notification send attempt.
 * Encapsulates both success and failure outcomes as a Result type,
 * allowing consumers to inspect the result without relying solely on exceptions.
 *
 * This design supports both:
 * - try-catch for unexpected/fatal errors (via exceptions)
 * - Result inspection for expected business outcomes (success/failure with details)
 */
@Getter
@Builder
public class NotificationResult {

    /**
     * Whether the notification was sent successfully.
     */
    private final boolean success;

    /**
     * The message ID assigned by the provider (e.g., SendGrid message ID).
     * Null if the send failed.
     */
    private final String messageId;

    /**
     * The name of the provider that handled the notification.
     */
    private final String providerName;

    /**
     * The channel type used for this notification.
     */
    private final ChannelType channelType;

    /**
     * Error details if the notification failed. Null on success.
     */
    private final ErrorDetail error;

    /**
     * Timestamp when the result was generated.
     */
    private final Instant timestamp;

    /**
     * Creates a successful result.
     */
    public static NotificationResult success(String messageId, String providerName, ChannelType channelType) {
        return NotificationResult.builder()
                .success(true)
                .messageId(messageId)
                .providerName(providerName)
                .channelType(channelType)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates a failure result with error details.
     */
    public static NotificationResult failure(String providerName, ChannelType channelType, String errorCode, String errorMessage) {
        return NotificationResult.builder()
                .success(false)
                .providerName(providerName)
                .channelType(channelType)
                .error(ErrorDetail.builder()
                        .code(errorCode)
                        .message(errorMessage)
                        .build())
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates a failure result from an exception.
     */
    public static NotificationResult failure(String providerName, ChannelType channelType, Exception exception) {
        return NotificationResult.builder()
                .success(false)
                .providerName(providerName)
                .channelType(channelType)
                .error(ErrorDetail.builder()
                        .code(exception.getClass().getSimpleName())
                        .message(exception.getMessage())
                        .build())
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Encapsulates error information for failed notifications.
     */
    @Getter
    @Builder
    public static class ErrorDetail {
        private final String code;
        private final String message;
    }
}
