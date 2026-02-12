package com.notification.lib.retry;

/**
 * Factory interface for creating {@link RetryExecutor} instances.
 * Allows consumers to customize retry behavior by providing their own
 * factory implementation via {@link com.notification.lib.config.NotificationConfig.Builder}.
 *
 * <p><b>Dependency Inversion Principle:</b> The core service depends on this
 * abstraction rather than directly instantiating {@link RetryExecutor}.</p>
 *
 * <p>The default factory creates a standard blocking {@link RetryExecutor}.
 * For non-blocking retry in async scenarios, consumers can provide a custom factory.</p>
 */
@FunctionalInterface
public interface RetryExecutorFactory {

    /**
     * Creates a RetryExecutor for the given policy and callback.
     *
     * @param policy the retry policy configuration
     * @param callback optional callback for retry events (may be null)
     * @return a configured RetryExecutor instance
     */
    RetryExecutor create(RetryPolicy policy, RetryExecutor.RetryEventCallback callback);
}
