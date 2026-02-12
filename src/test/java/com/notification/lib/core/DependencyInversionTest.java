package com.notification.lib.core;

import com.notification.lib.channel.email.EmailMessage;
import com.notification.lib.config.NotificationConfig;
import com.notification.lib.event.NotificationEventPublisher;
import com.notification.lib.event.NotificationListener;
import com.notification.lib.exception.SendException;
import com.notification.lib.retry.RetryExecutor;
import com.notification.lib.retry.RetryExecutorFactory;
import com.notification.lib.retry.RetryPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests that verify the Dependency Inversion improvements:
 * - Custom event publisher injection
 * - Custom retry executor factory injection
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Dependency Inversion - Custom Injection")
class DependencyInversionTest {

    @Mock
    private NotificationChannel<EmailMessage> emailChannel;

    private EmailMessage validEmail() {
        return EmailMessage.builder()
                .recipient("user@example.com")
                .from("noreply@company.com")
                .subject("Test")
                .body("Body")
                .build();
    }

    @Test
    @DisplayName("should use injected event publisher")
    void shouldUseInjectedEventPublisher() {
        when(emailChannel.getChannelType()).thenReturn(ChannelType.EMAIL);
        when(emailChannel.send(any())).thenReturn(
                NotificationResult.success("msg-1", "TestProvider", ChannelType.EMAIL));

        List<String> events = new ArrayList<>();
        NotificationEventPublisher customPublisher = new NotificationEventPublisher();
        customPublisher.addListener(event -> events.add(event.getStatus().name()));

        NotificationConfig config = NotificationConfig.builder()
                .registerChannel(ChannelType.EMAIL, emailChannel)
                .withEventPublisher(customPublisher)
                .build();

        NotificationService service = new NotificationService(config);
        service.send(validEmail());

        assertThat(events).contains("PENDING", "SENDING", "SENT");
    }

    @Test
    @DisplayName("should use injected retry executor factory")
    void shouldUseInjectedRetryExecutorFactory() {
        AtomicInteger factoryCallCount = new AtomicInteger(0);
        AtomicInteger sendAttempts = new AtomicInteger(0);

        when(emailChannel.getChannelType()).thenReturn(ChannelType.EMAIL);
        when(emailChannel.send(any())).thenAnswer(inv -> {
            int attempt = sendAttempts.incrementAndGet();
            if (attempt <= 1) {
                throw new SendException("Temporary failure", "TestProvider");
            }
            return NotificationResult.success("msg-1", "TestProvider", ChannelType.EMAIL);
        });

        RetryExecutorFactory customFactory = (policy, callback) -> {
            factoryCallCount.incrementAndGet();
            return new RetryExecutor(policy, callback);
        };

        NotificationConfig config = NotificationConfig.builder()
                .registerChannel(ChannelType.EMAIL, emailChannel)
                .withRetryPolicy(RetryPolicy.builder().maxRetries(2).initialDelayMs(10).build())
                .withRetryExecutorFactory(customFactory)
                .build();

        NotificationService service = new NotificationService(config);
        NotificationResult result = service.send(validEmail());

        assertThat(result.isSuccess()).isTrue();
        assertThat(factoryCallCount.get()).isEqualTo(1);
        assertThat(sendAttempts.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("should fallback to default publisher when none injected")
    void shouldFallbackToDefaultPublisher() {
        when(emailChannel.getChannelType()).thenReturn(ChannelType.EMAIL);
        when(emailChannel.send(any())).thenReturn(
                NotificationResult.success("msg-1", "TestProvider", ChannelType.EMAIL));

        List<String> events = new ArrayList<>();
        NotificationListener listener = event -> events.add(event.getStatus().name());

        NotificationConfig config = NotificationConfig.builder()
                .registerChannel(ChannelType.EMAIL, emailChannel)
                .addListener(listener)
                .build();

        NotificationService service = new NotificationService(config);
        service.send(validEmail());

        // Default publisher should still work with listeners from config
        assertThat(events).contains("PENDING", "SENDING", "SENT");
    }
}
