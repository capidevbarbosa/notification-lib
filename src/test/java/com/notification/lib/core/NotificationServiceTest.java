package com.notification.lib.core;

import com.notification.lib.channel.email.EmailMessage;
import com.notification.lib.config.NotificationConfig;
import com.notification.lib.event.NotificationEvent;
import com.notification.lib.event.NotificationListener;
import com.notification.lib.event.NotificationStatus;
import com.notification.lib.exception.NotificationException;
import com.notification.lib.exception.SendException;
import com.notification.lib.exception.ValidationException;
import com.notification.lib.retry.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NotificationService")
class NotificationServiceTest {

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

    @Nested
    @DisplayName("Basic send operations")
    class BasicSend {

        private NotificationService service;

        @BeforeEach
        void setUp() {
            when(emailChannel.getChannelType()).thenReturn(ChannelType.EMAIL);
            NotificationConfig config = NotificationConfig.builder()
                    .registerChannel(ChannelType.EMAIL, emailChannel)
                    .build();
            service = new NotificationService(config);
        }

        @Test
        @DisplayName("should send notification through correct channel")
        void shouldSendThroughCorrectChannel() {
            NotificationResult expected = NotificationResult.success("msg-1", "TestProvider", ChannelType.EMAIL);
            when(emailChannel.send(any())).thenReturn(expected);

            NotificationResult result = service.send(validEmail());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessageId()).isEqualTo("msg-1");
            verify(emailChannel).send(any(EmailMessage.class));
        }

        @Test
        @DisplayName("should throw when no channel registered for type")
        void shouldThrowWhenNoChannelRegistered() {
            // SMS channel is not registered
            com.notification.lib.channel.sms.SmsMessage sms = com.notification.lib.channel.sms.SmsMessage.builder()
                    .recipient("+1234567890")
                    .body("Test")
                    .fromNumber("+0987654321")
                    .build();

            assertThatThrownBy(() -> service.send(sms))
                    .isInstanceOf(NotificationException.class)
                    .hasMessageContaining("No channel registered");
        }

        @Test
        @DisplayName("should throw when message is null")
        void shouldThrowWhenMessageNull() {
            assertThatThrownBy(() -> service.send(null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("null");
        }

        @Test
        @DisplayName("should propagate ValidationException from channel")
        void shouldPropagateValidationException() {
            when(emailChannel.send(any())).thenThrow(new ValidationException("Bad email"));

            assertThatThrownBy(() -> service.send(validEmail()))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("should propagate SendException from channel")
        void shouldPropagateSendException() {
            when(emailChannel.send(any())).thenThrow(new SendException("Provider down", "TestProvider"));

            assertThatThrownBy(() -> service.send(validEmail()))
                    .isInstanceOf(SendException.class);
        }
    }

    @Nested
    @DisplayName("Event publishing")
    class EventPublishing {

        @Test
        @DisplayName("should publish lifecycle events on successful send")
        void shouldPublishEventsOnSuccess() {
            when(emailChannel.getChannelType()).thenReturn(ChannelType.EMAIL);

            List<NotificationEvent> events = new ArrayList<>();
            NotificationListener listener = events::add;

            NotificationConfig config = NotificationConfig.builder()
                    .registerChannel(ChannelType.EMAIL, emailChannel)
                    .addListener(listener)
                    .build();
            NotificationService service = new NotificationService(config);

            when(emailChannel.send(any())).thenReturn(
                    NotificationResult.success("msg-1", "TestProvider", ChannelType.EMAIL));

            service.send(validEmail());

            assertThat(events).extracting(NotificationEvent::getStatus)
                    .containsExactly(
                            NotificationStatus.PENDING,
                            NotificationStatus.SENDING,
                            NotificationStatus.SENT
                    );
        }

        @Test
        @DisplayName("should publish FAILED event on send exception")
        void shouldPublishFailedOnException() {
            when(emailChannel.getChannelType()).thenReturn(ChannelType.EMAIL);

            List<NotificationEvent> events = new ArrayList<>();
            NotificationListener listener = events::add;

            NotificationConfig config = NotificationConfig.builder()
                    .registerChannel(ChannelType.EMAIL, emailChannel)
                    .addListener(listener)
                    .build();
            NotificationService service = new NotificationService(config);

            when(emailChannel.send(any())).thenThrow(new SendException("Error", "TestProvider"));

            try {
                service.send(validEmail());
            } catch (SendException ignored) {
            }

            assertThat(events).extracting(NotificationEvent::getStatus)
                    .contains(NotificationStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("Retry integration")
    class RetryIntegration {

        @Test
        @DisplayName("should retry on SendException and succeed")
        void shouldRetryAndSucceed() {
            when(emailChannel.getChannelType()).thenReturn(ChannelType.EMAIL);

            NotificationConfig config = NotificationConfig.builder()
                    .registerChannel(ChannelType.EMAIL, emailChannel)
                    .withRetryPolicy(RetryPolicy.builder()
                            .maxRetries(2)
                            .initialDelayMs(10) // Fast for testing
                            .build())
                    .build();
            NotificationService service = new NotificationService(config);

            // Fail first time, succeed second time
            when(emailChannel.send(any()))
                    .thenThrow(new SendException("Temporary error", "TestProvider"))
                    .thenReturn(NotificationResult.success("msg-1", "TestProvider", ChannelType.EMAIL));

            NotificationResult result = service.send(validEmail());

            assertThat(result.isSuccess()).isTrue();
            verify(emailChannel, times(2)).send(any());
        }
    }
}
