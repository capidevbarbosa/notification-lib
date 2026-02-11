package com.notification.lib.exception;

/**
 * Base exception for all notification-related errors.
 * Provides a common ancestor for more specific exception types.
 */
public class NotificationException extends RuntimeException {

    public NotificationException(String message) {
        super(message);
    }

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
