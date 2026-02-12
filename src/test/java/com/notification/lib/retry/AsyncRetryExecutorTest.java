package com.notification.lib.retry;

import com.notification.lib.core.ChannelType;
import com.notification.lib.core.NotificationResult;
import com.notification.lib.exception.SendException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the non-blocking AsyncRetryExecutor.
 */
@DisplayName("AsyncRetryExecutor")
class AsyncRetryExecutorTest {

    private ScheduledExecutorService scheduler;

    @BeforeEach
    void setUp() {
        scheduler = Executors.newScheduledThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdownNow();
    }

    @Test
    @DisplayName("should succeed on first attempt without retry")
    void shouldSucceedOnFirstAttempt() throws Exception {
        RetryPolicy policy = RetryPolicy.builder().maxRetries(3).initialDelayMs(10).build();
        AsyncRetryExecutor executor = new AsyncRetryExecutor(policy, scheduler);

        CompletableFuture<NotificationResult> future = executor.executeWithRetryAsync(
                () -> NotificationResult.success("msg-1", "TestProvider", ChannelType.EMAIL));

        NotificationResult result = future.get(5, TimeUnit.SECONDS);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessageId()).isEqualTo("msg-1");
    }

    @Test
    @DisplayName("should retry and succeed after transient failures")
    void shouldRetryAndSucceed() throws Exception {
        RetryPolicy policy = RetryPolicy.builder().maxRetries(3).initialDelayMs(10).build();
        AsyncRetryExecutor executor = new AsyncRetryExecutor(policy, scheduler);
        AtomicInteger attempts = new AtomicInteger(0);

        CompletableFuture<NotificationResult> future = executor.executeWithRetryAsync(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt <= 2) {
                throw new SendException("Transient failure", "TestProvider");
            }
            return NotificationResult.success("msg-1", "TestProvider", ChannelType.EMAIL);
        });

        NotificationResult result = future.get(5, TimeUnit.SECONDS);
        assertThat(result.isSuccess()).isTrue();
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("should fail after all retries exhausted")
    void shouldFailAfterRetriesExhausted() {
        RetryPolicy policy = RetryPolicy.builder().maxRetries(2).initialDelayMs(10).build();
        AsyncRetryExecutor executor = new AsyncRetryExecutor(policy, scheduler);

        CompletableFuture<NotificationResult> future = executor.executeWithRetryAsync(
                () -> { throw new SendException("Permanent failure", "TestProvider"); });

        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(SendException.class);
    }

    @Test
    @DisplayName("should invoke callback on retries")
    void shouldInvokeCallbackOnRetries() throws Exception {
        RetryPolicy policy = RetryPolicy.builder().maxRetries(2).initialDelayMs(10).build();
        AtomicInteger callbackCount = new AtomicInteger(0);
        AsyncRetryExecutor executor = new AsyncRetryExecutor(policy, scheduler,
                (attempt, maxRetries) -> callbackCount.incrementAndGet());
        AtomicInteger attempts = new AtomicInteger(0);

        CompletableFuture<NotificationResult> future = executor.executeWithRetryAsync(() -> {
            if (attempts.incrementAndGet() <= 1) {
                throw new SendException("Fail", "TestProvider");
            }
            return NotificationResult.success("msg-1", "TestProvider", ChannelType.EMAIL);
        });

        future.get(5, TimeUnit.SECONDS);
        assertThat(callbackCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("should calculate exponential backoff delay")
    void shouldCalculateExponentialDelay() {
        RetryPolicy policy = RetryPolicy.builder()
                .maxRetries(5).initialDelayMs(100).multiplier(2.0).maxDelayMs(5000)
                .build();
        AsyncRetryExecutor executor = new AsyncRetryExecutor(policy, scheduler);

        assertThat(executor.calculateDelay(1)).isEqualTo(100);
        assertThat(executor.calculateDelay(2)).isEqualTo(200);
        assertThat(executor.calculateDelay(3)).isEqualTo(400);
    }

    @Test
    @DisplayName("asBlockingExecutor should wrap async behavior for sync API")
    void asBlockingExecutorShouldWork() {
        RetryPolicy policy = RetryPolicy.builder().maxRetries(1).initialDelayMs(10).build();
        AsyncRetryExecutor asyncExecutor = new AsyncRetryExecutor(policy, scheduler);
        RetryExecutor blockingExecutor = asyncExecutor.asBlockingExecutor();

        AtomicInteger attempts = new AtomicInteger(0);
        NotificationResult result = blockingExecutor.executeWithRetry(() -> {
            if (attempts.incrementAndGet() <= 1) {
                throw new SendException("Transient", "TestProvider");
            }
            return NotificationResult.success("msg-1", "TestProvider", ChannelType.EMAIL);
        });

        assertThat(result.isSuccess()).isTrue();
        assertThat(attempts.get()).isEqualTo(2);
    }
}
