package com.notification.lib.event;

import com.notification.lib.core.ChannelType;
import com.notification.lib.core.NotificationResult;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Event emitted during the notification lifecycle.
 * Consumers can subscribe to these events to track notification status,
 * log metrics, or trigger side effects.
 */
@Getter
@Builder
public class NotificationEvent {

    /**
     * The message ID being tracked.
     */
    private final String messageId;

    /**
     * The channel through which the notification was sent.
     */
    private final ChannelType channelType;

    /**
     * The current status of the notification.
     */
    private final NotificationStatus status;

    /**
     * The notification result (available when status is SENT or FAILED).
     */
    private final NotificationResult result;

    /**
     * Optional error message when status is FAILED.
     */
    private final String errorMessage;

    /**
     * The current retry attempt number (0 if not retrying).
     */
    private final int retryAttempt;

    /**
     * Timestamp of the event.
     */
    private final Instant timestamp;

    public static NotificationEvent pending(String messageId, ChannelType channelType) {
        return NotificationEvent.builder()
                .messageId(messageId)
                .channelType(channelType)
                .status(NotificationStatus.PENDING)
                .timestamp(Instant.now())
                .build();
    }

    public static NotificationEvent sending(String messageId, ChannelType channelType) {
        return NotificationEvent.builder()
                .messageId(messageId)
                .channelType(channelType)
                .status(NotificationStatus.SENDING)
                .timestamp(Instant.now())
                .build();
    }

    public static NotificationEvent sent(String messageId, ChannelType channelType, NotificationResult result) {
        return NotificationEvent.builder()
                .messageId(messageId)
                .channelType(channelType)
                .status(NotificationStatus.SENT)
                .result(result)
                .timestamp(Instant.now())
                .build();
    }

    public static NotificationEvent failed(String messageId, ChannelType channelType, String errorMessage) {
        return NotificationEvent.builder()
                .messageId(messageId)
                .channelType(channelType)
                .status(NotificationStatus.FAILED)
                .errorMessage(errorMessage)
                .timestamp(Instant.now())
                .build();
    }

    public static NotificationEvent retrying(String messageId, ChannelType channelType, int attempt) {
        return NotificationEvent.builder()
                .messageId(messageId)
                .channelType(channelType)
                .status(NotificationStatus.RETRYING)
                .retryAttempt(attempt)
                .timestamp(Instant.now())
                .build();
    }
}
