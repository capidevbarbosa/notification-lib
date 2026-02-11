package com.notification.lib.retry;

import com.notification.lib.core.ChannelType;
import com.notification.lib.core.NotificationResult;
import com.notification.lib.exception.SendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RetryExecutor")
class RetryExecutorTest {

    @Test
    @DisplayName("should succeed on first attempt without retry")
    void shouldSucceedOnFirstAttempt() {
        RetryPolicy policy = RetryPolicy.builder().maxRetries(3).initialDelayMs(10).build();
        RetryExecutor executor = new RetryExecutor(policy);

        AtomicInteger attempts = new AtomicInteger(0);

        NotificationResult result = executor.executeWithRetry(() -> {
            attempts.incrementAndGet();
            return NotificationResult.success("msg-1", "Test", ChannelType.EMAIL);
        });

        assertThat(result.isSuccess()).isTrue();
        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("should retry and succeed after failures")
    void shouldRetryAndSucceed() {
        RetryPolicy policy = RetryPolicy.builder().maxRetries(3).initialDelayMs(10).build();
        RetryExecutor executor = new RetryExecutor(policy);

        AtomicInteger attempts = new AtomicInteger(0);

        NotificationResult result = executor.executeWithRetry(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new SendException("Temporary failure", "TestProvider");
            }
            return NotificationResult.success("msg-1", "Test", ChannelType.EMAIL);
        });

        assertThat(result.isSuccess()).isTrue();
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("should throw after all retries exhausted")
    void shouldThrowAfterRetriesExhausted() {
        RetryPolicy policy = RetryPolicy.builder().maxRetries(2).initialDelayMs(10).build();
        RetryExecutor executor = new RetryExecutor(policy);

        assertThatThrownBy(() -> executor.executeWithRetry(() -> {
            throw new SendException("Permanent failure", "TestProvider");
        })).isInstanceOf(SendException.class)
                .hasMessageContaining("Permanent failure");
    }

    @Test
    @DisplayName("should calculate exponential backoff delay")
    void shouldCalculateExponentialDelay() {
        RetryPolicy policy = RetryPolicy.builder()
                .initialDelayMs(1000)
                .multiplier(2.0)
                .maxDelayMs(30000)
                .build();
        RetryExecutor executor = new RetryExecutor(policy);

        assertThat(executor.calculateDelay(1)).isEqualTo(1000);
        assertThat(executor.calculateDelay(2)).isEqualTo(2000);
        assertThat(executor.calculateDelay(3)).isEqualTo(4000);
        assertThat(executor.calculateDelay(4)).isEqualTo(8000);
    }

    @Test
    @DisplayName("should cap delay at maxDelayMs")
    void shouldCapDelay() {
        RetryPolicy policy = RetryPolicy.builder()
                .initialDelayMs(1000)
                .multiplier(10.0)
                .maxDelayMs(5000)
                .build();
        RetryExecutor executor = new RetryExecutor(policy);

        assertThat(executor.calculateDelay(5)).isEqualTo(5000);
    }

    @Test
    @DisplayName("should invoke callback on retry")
    void shouldInvokeCallbackOnRetry() {
        RetryPolicy policy = RetryPolicy.builder().maxRetries(2).initialDelayMs(10).build();
        AtomicInteger callbackCount = new AtomicInteger(0);

        RetryExecutor executor = new RetryExecutor(policy,
                (attempt, max) -> callbackCount.incrementAndGet());

        AtomicInteger attempts = new AtomicInteger(0);

        executor.executeWithRetry(() -> {
            if (attempts.incrementAndGet() <= 2) {
                throw new SendException("Fail", "Test");
            }
            return NotificationResult.success("msg-1", "Test", ChannelType.EMAIL);
        });

        assertThat(callbackCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("should work with noRetry policy")
    void shouldWorkWithNoRetry() {
        RetryPolicy policy = RetryPolicy.noRetry();
        RetryExecutor executor = new RetryExecutor(policy);

        AtomicInteger attempts = new AtomicInteger(0);

        assertThatThrownBy(() -> executor.executeWithRetry(() -> {
            attempts.incrementAndGet();
            throw new SendException("Fail", "Test");
        })).isInstanceOf(SendException.class);

        assertThat(attempts.get()).isEqualTo(1);
    }
}
