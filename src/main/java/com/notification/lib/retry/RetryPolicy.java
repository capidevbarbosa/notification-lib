package com.notification.lib.retry;

import lombok.Builder;
import lombok.Getter;

/**
 * Configuration for the retry mechanism.
 * Supports exponential backoff with configurable parameters.
 *
 * <p>Example: With maxRetries=3, initialDelayMs=1000, multiplier=2.0:
 * - Attempt 1: wait 1000ms
 * - Attempt 2: wait 2000ms
 * - Attempt 3: wait 4000ms</p>
 */
@Getter
@Builder
public class RetryPolicy {

    /**
     * Maximum number of retry attempts. Default: 3.
     */
    @Builder.Default
    private final int maxRetries = 3;

    /**
     * Initial delay in milliseconds before the first retry. Default: 1000ms.
     */
    @Builder.Default
    private final long initialDelayMs = 1000;

    /**
     * Multiplier for exponential backoff. Default: 2.0.
     */
    @Builder.Default
    private final double multiplier = 2.0;

    /**
     * Maximum delay cap in milliseconds. Default: 30000ms (30 seconds).
     */
    @Builder.Default
    private final long maxDelayMs = 30000;

    /**
     * Creates a default retry policy with 3 retries and exponential backoff.
     */
    public static RetryPolicy defaultPolicy() {
        return RetryPolicy.builder().build();
    }

    /**
     * Creates a retry policy with no retries (disabled).
     */
    public static RetryPolicy noRetry() {
        return RetryPolicy.builder().maxRetries(0).build();
    }
}
