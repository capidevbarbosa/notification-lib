package com.notification.lib.event;

/**
 * Represents the lifecycle states of a notification.
 */
public enum NotificationStatus {
    PENDING,
    SENDING,
    SENT,
    FAILED,
    RETRYING
}
