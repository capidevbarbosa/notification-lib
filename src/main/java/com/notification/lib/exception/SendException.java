package com.notification.lib.exception;

/**
 * Thrown when a notification fails during the actual send operation.
 * This represents a provider-level or transport-level failure,
 * distinct from validation errors.
 *
 * <p>Examples: network timeout, provider API error, rate limiting, etc.</p>
 */
public class SendException extends NotificationException {

    private final String providerName;
    private final String errorCode;

    public SendException(String message, String providerName) {
        super(message);
        this.providerName = providerName;
        this.errorCode = null;
    }

    public SendException(String message, String providerName, String errorCode) {
        super(message);
        this.providerName = providerName;
        this.errorCode = errorCode;
    }

    public SendException(String message, String providerName, Throwable cause) {
        super(message, cause);
        this.providerName = providerName;
        this.errorCode = null;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
