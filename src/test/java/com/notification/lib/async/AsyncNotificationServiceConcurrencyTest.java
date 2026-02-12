package com.notification.lib.async;

import com.notification.lib.channel.email.EmailMessage;
import com.notification.lib.core.ChannelType;
import com.notification.lib.core.NotificationChannel;
import com.notification.lib.core.NotificationResult;
import com.notification.lib.core.NotificationService;
import com.notification.lib.config.NotificationConfig;
import com.notification.lib.exception.SendException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Concurrency tests for batch notification sending.
 * Verifies that batch operations actually execute in parallel
 * and handle concurrent scenarios correctly.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AsyncNotificationService - Concurrency")
class AsyncNotificationServiceConcurrencyTest {

    @Mock
    private NotificationChannel<EmailMessage> emailChannel;

    private AsyncNotificationService asyncService;

    @BeforeEach
    void setUp() {
        when(emailChannel.getChannelType()).thenReturn(ChannelType.EMAIL);

        NotificationConfig config = NotificationConfig.builder()
                .registerChannel(ChannelType.EMAIL, emailChannel)
                .build();
        NotificationService service = new NotificationService(config);
        asyncService = new AsyncNotificationService(service);
    }

    private EmailMessage validEmail() {
        return EmailMessage.builder()
                .recipient("user@example.com")
                .from("noreply@company.com")
                .subject("Test")
                .body("Body")
                .build();
    }

    @Test
    @DisplayName("should execute batch items in parallel")
    void shouldExecuteBatchInParallel() throws Exception {
        AtomicInteger concurrentCount = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        CountDownLatch allStarted = new CountDownLatch(3);

        when(emailChannel.send(any())).thenAnswer(invocation -> {
            int current = concurrentCount.incrementAndGet();
            maxConcurrent.updateAndGet(prev -> Math.max(prev, current));
            allStarted.countDown();

            // Wait for all to be running concurrently
            allStarted.await(2, TimeUnit.SECONDS);

            // Small delay to simulate work
            Thread.sleep(50);
            concurrentCount.decrementAndGet();

            return NotificationResult.success("msg-" + current, "TestProvider", ChannelType.EMAIL);
        });

        List<EmailMessage> messages = List.of(validEmail(), validEmail(), validEmail());
        List<NotificationResult> results = asyncService.sendBatch(messages).get(5, TimeUnit.SECONDS);

        assertThat(results).hasSize(3);
        assertThat(results).allMatch(NotificationResult::isSuccess);
        // At least 2 should have run concurrently (depends on thread pool, but cached pool should allow all 3)
        assertThat(maxConcurrent.get()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("should handle mixed success and failure in concurrent batch settled")
    void shouldHandleMixedResultsInConcurrentBatchSettled() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);

        when(emailChannel.send(any())).thenAnswer(invocation -> {
            int count = callCount.incrementAndGet();
            Thread.sleep(20); // simulate work
            if (count % 2 == 0) {
                throw new SendException("Simulated failure " + count, "TestProvider");
            }
            return NotificationResult.success("msg-" + count, "TestProvider", ChannelType.EMAIL);
        });

        List<EmailMessage> messages = IntStream.range(0, 6)
                .mapToObj(i -> validEmail())
                .toList();

        List<NotificationResult> results = asyncService.sendBatchSettled(messages)
                .get(10, TimeUnit.SECONDS);

        assertThat(results).hasSize(6);

        long successCount = results.stream().filter(NotificationResult::isSuccess).count();
        long failureCount = results.stream().filter(r -> !r.isSuccess()).count();

        assertThat(successCount).isGreaterThan(0);
        assertThat(failureCount).isGreaterThan(0);
        assertThat(successCount + failureCount).isEqualTo(6);
    }

    @Test
    @DisplayName("should complete batch within timeout for large batch")
    void shouldCompleteLargeBatchWithinTimeout() throws Exception {
        when(emailChannel.send(any())).thenAnswer(invocation -> {
            Thread.sleep(10); // simulate fast work
            return NotificationResult.success("msg", "TestProvider", ChannelType.EMAIL);
        });

        List<EmailMessage> messages = IntStream.range(0, 20)
                .mapToObj(i -> validEmail())
                .toList();

        List<NotificationResult> results = asyncService.sendBatch(messages)
                .get(10, TimeUnit.SECONDS);

        assertThat(results).hasSize(20);
        assertThat(results).allMatch(NotificationResult::isSuccess);
    }
}
