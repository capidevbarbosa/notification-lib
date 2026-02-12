package com.notification.lib.retry;

import com.notification.lib.core.NotificationResult;
import com.notification.lib.exception.SendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Executes a notification send operation with retry logic.
 * Implements exponential backoff based on the configured {@link RetryPolicy}.
 *
 * <p><b>Important - Blocking behavior:</b> This executor uses {@code Thread.sleep()}
 * for backoff delays, making it <b>blocking</b>. When used within
 * {@link com.notification.lib.async.AsyncNotificationService}, the retry delay
 * will block a thread from the executor's thread pool, which may reduce throughput
 * in high-concurrency scenarios.</p>
 *
 * <p>For non-blocking retry in async pipelines, use {@link AsyncRetryExecutor} or
 * provide a custom {@link RetryExecutorFactory} via
 * {@link com.notification.lib.config.NotificationConfig.Builder#withRetryExecutorFactory}.</p>
 *
 * <p>Only retries on {@link SendException} (transport/provider failures).
 * Validation errors are NOT retried since they indicate bad input.</p>
 */
public class RetryExecutor {

    private static final Logger log = LoggerFactory.getLogger(RetryExecutor.class);

    private final RetryPolicy policy;

    /**
     * Listener callback for retry events. Can be used to publish events.
     */
    private final RetryEventCallback callback;

    public RetryExecutor(RetryPolicy policy) {
        this(policy, null);
    }

    public RetryExecutor(RetryPolicy policy, RetryEventCallback callback) {
        this.policy = policy;
        this.callback = callback;
    }

    /**
     * Executes the given operation with retry logic.
     *
     * @param operation the send operation to execute
     * @return the notification result from a successful attempt
     * @throws SendException if all retry attempts are exhausted
     */
    public NotificationResult executeWithRetry(Supplier<NotificationResult> operation) {
        int attempt = 0;
        SendException lastException = null;

        while (attempt <= policy.getMaxRetries()) {
            try {
                if (attempt > 0) {
                    long delay = calculateDelay(attempt);
                    log.info("Retry attempt {}/{} after {}ms delay", attempt, policy.getMaxRetries(), delay);

                    if (callback != null) {
                        callback.onRetry(attempt, policy.getMaxRetries());
                    }

                    Thread.sleep(delay);
                }

                return operation.get();

            } catch (SendException e) {
                lastException = e;
                log.warn("Send attempt {} failed: {}", attempt + 1, e.getMessage());
                attempt++;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SendException("Retry interrupted", "RetryExecutor", e);
            }
        }

        log.error("All {} retry attempts exhausted", policy.getMaxRetries() + 1);
        throw lastException;
    }

    /**
     * Calculates the delay for the given attempt using exponential backoff.
     */
    long calculateDelay(int attempt) {
        long delay = (long) (policy.getInitialDelayMs() * Math.pow(policy.getMultiplier(), attempt - 1));
        return Math.min(delay, policy.getMaxDelayMs());
    }

    /**
     * Callback interface for retry events.
     */
    @FunctionalInterface
    public interface RetryEventCallback {
        void onRetry(int currentAttempt, int maxRetries);
    }
}
