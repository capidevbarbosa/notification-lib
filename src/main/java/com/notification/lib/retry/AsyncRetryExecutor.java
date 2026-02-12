package com.notification.lib.retry;

import com.notification.lib.core.NotificationResult;
import com.notification.lib.exception.SendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Non-blocking retry executor that uses a {@link ScheduledExecutorService}
 * for backoff delays instead of {@code Thread.sleep()}.
 *
 * <p>Suitable for use within async pipelines where blocking a thread during
 * backoff is undesirable. Unlike {@link RetryExecutor}, this implementation
 * schedules delayed retries without holding a thread during the wait.</p>
 *
 * <p>Example usage with {@link com.notification.lib.config.NotificationConfig}:</p>
 * <pre>
 * ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
 *
 * NotificationConfig config = NotificationConfig.builder()
 *     .registerChannel(ChannelType.EMAIL, emailChannel)
 *     .withRetryPolicy(RetryPolicy.defaultPolicy())
 *     .withRetryExecutorFactory((policy, callback) ->
 *         new AsyncRetryExecutor(policy, scheduler, callback).asBlockingExecutor())
 *     .build();
 * </pre>
 *
 * @see RetryExecutor for the blocking alternative
 * @see RetryExecutorFactory for injecting custom retry strategies
 */
public class AsyncRetryExecutor {

    private static final Logger log = LoggerFactory.getLogger(AsyncRetryExecutor.class);

    private final RetryPolicy policy;
    private final ScheduledExecutorService scheduler;
    private final RetryExecutor.RetryEventCallback callback;

    public AsyncRetryExecutor(RetryPolicy policy, ScheduledExecutorService scheduler) {
        this(policy, scheduler, null);
    }

    public AsyncRetryExecutor(RetryPolicy policy, ScheduledExecutorService scheduler,
                               RetryExecutor.RetryEventCallback callback) {
        if (policy == null) {
            throw new IllegalArgumentException("RetryPolicy must not be null");
        }
        if (scheduler == null) {
            throw new IllegalArgumentException("ScheduledExecutorService must not be null");
        }
        this.policy = policy;
        this.scheduler = scheduler;
        this.callback = callback;
    }

    /**
     * Executes the given operation with non-blocking retry logic.
     * Returns a CompletableFuture that completes with the result or fails
     * after all retries are exhausted.
     *
     * @param operation the send operation to execute
     * @return a CompletableFuture with the notification result
     */
    public CompletableFuture<NotificationResult> executeWithRetryAsync(Supplier<NotificationResult> operation) {
        return attemptAsync(operation, 0, null);
    }

    private CompletableFuture<NotificationResult> attemptAsync(
            Supplier<NotificationResult> operation, int attempt, SendException lastException) {

        if (attempt > policy.getMaxRetries()) {
            log.error("All {} retry attempts exhausted", policy.getMaxRetries() + 1);
            return CompletableFuture.failedFuture(lastException);
        }

        if (attempt > 0) {
            long delay = calculateDelay(attempt);
            log.info("Async retry attempt {}/{} after {}ms delay", attempt, policy.getMaxRetries(), delay);

            if (callback != null) {
                callback.onRetry(attempt, policy.getMaxRetries());
            }

            CompletableFuture<NotificationResult> delayedFuture = new CompletableFuture<>();
            scheduler.schedule(() -> {
                try {
                    NotificationResult result = operation.get();
                    delayedFuture.complete(result);
                } catch (SendException e) {
                    log.warn("Async send attempt {} failed: {}", attempt + 1, e.getMessage());
                    attemptAsync(operation, attempt + 1, e)
                            .whenComplete((r, ex) -> {
                                if (ex != null) delayedFuture.completeExceptionally(ex);
                                else delayedFuture.complete(r);
                            });
                } catch (Exception e) {
                    delayedFuture.completeExceptionally(e);
                }
            }, delay, TimeUnit.MILLISECONDS);

            return delayedFuture;
        }

        // First attempt - execute immediately
        try {
            return CompletableFuture.completedFuture(operation.get());
        } catch (SendException e) {
            log.warn("Async send attempt 1 failed: {}", e.getMessage());
            return attemptAsync(operation, 1, e);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Calculates the delay for the given attempt using exponential backoff.
     */
    long calculateDelay(int attempt) {
        long delay = (long) (policy.getInitialDelayMs() * Math.pow(policy.getMultiplier(), attempt - 1));
        return Math.min(delay, policy.getMaxDelayMs());
    }

    /**
     * Returns a blocking {@link RetryExecutor} adapter that delegates to this async executor.
     * Useful for integrating with the synchronous {@link com.notification.lib.core.NotificationService}
     * while still using scheduled (non-sleep) delays.
     *
     * <p><b>Note:</b> The returned executor will block the calling thread while waiting
     * for the CompletableFuture to complete, but the backoff delays themselves are
     * non-blocking (scheduled rather than sleeping).</p>
     *
     * @return a RetryExecutor that wraps this async executor
     */
    public RetryExecutor asBlockingExecutor() {
        return new RetryExecutor(policy, callback) {
            @Override
            public NotificationResult executeWithRetry(Supplier<NotificationResult> operation) {
                try {
                    return executeWithRetryAsync(operation).join();
                } catch (java.util.concurrent.CompletionException e) {
                    if (e.getCause() instanceof SendException se) {
                        throw se;
                    }
                    throw e;
                }
            }
        };
    }
}
