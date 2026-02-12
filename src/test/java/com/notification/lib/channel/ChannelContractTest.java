package com.notification.lib.channel;

import com.notification.lib.channel.email.EmailChannel;
import com.notification.lib.channel.email.EmailMessage;
import com.notification.lib.channel.email.provider.EmailProvider;
import com.notification.lib.channel.push.PushChannel;
import com.notification.lib.channel.push.PushMessage;
import com.notification.lib.channel.push.provider.PushProvider;
import com.notification.lib.channel.sms.SmsChannel;
import com.notification.lib.channel.sms.SmsMessage;
import com.notification.lib.channel.sms.provider.SmsProvider;
import com.notification.lib.core.ChannelMessage;
import com.notification.lib.core.ChannelType;
import com.notification.lib.core.NotificationChannel;
import com.notification.lib.core.NotificationResult;
import com.notification.lib.exception.SendException;
import com.notification.lib.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Contract tests that verify all notification channels follow the same behavioral contract:
 * <ul>
 *   <li>Valid messages are sent successfully through the provider</li>
 *   <li>Invalid messages throw ValidationException before reaching the provider</li>
 *   <li>Provider exceptions (SendException) propagate to the caller</li>
 *   <li>Channel type and provider name are reported correctly</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Channel Contract Tests")
class ChannelContractTest {

    // =========================================================================
    // Helper: creates a channel + valid message + invalid message for each type
    // =========================================================================

    private record ChannelFixture<T extends ChannelMessage>(
            NotificationChannel<T> channel,
            T validMessage,
            T invalidMessage,
            ChannelType expectedType,
            String expectedProvider
    ) {}

    private ChannelFixture<EmailMessage> emailFixture() {
        EmailProvider provider = mock(EmailProvider.class);
        when(provider.getName()).thenReturn("MockEmail");
        when(provider.send(any())).thenReturn(
                NotificationResult.success("email-1", "MockEmail", ChannelType.EMAIL));
        return new ChannelFixture<>(
                new EmailChannel(provider),
                EmailMessage.builder()
                        .recipient("user@example.com")
                        .from("noreply@example.com")
                        .subject("Test")
                        .body("Hello")
                        .build(),
                EmailMessage.builder()
                        .recipient("invalid-email")
                        .build(),
                ChannelType.EMAIL,
                "MockEmail"
        );
    }

    private ChannelFixture<SmsMessage> smsFixture() {
        SmsProvider provider = mock(SmsProvider.class);
        when(provider.getName()).thenReturn("MockSms");
        when(provider.send(any())).thenReturn(
                NotificationResult.success("sms-1", "MockSms", ChannelType.SMS));
        return new ChannelFixture<>(
                new SmsChannel(provider),
                SmsMessage.builder()
                        .recipient("+1234567890")
                        .fromNumber("+0987654321")
                        .body("Test SMS")
                        .build(),
                SmsMessage.builder()
                        .recipient("not-a-phone")
                        .build(),
                ChannelType.SMS,
                "MockSms"
        );
    }

    private ChannelFixture<PushMessage> pushFixture() {
        PushProvider provider = mock(PushProvider.class);
        when(provider.getName()).thenReturn("MockPush");
        when(provider.send(any())).thenReturn(
                NotificationResult.success("push-1", "MockPush", ChannelType.PUSH));
        return new ChannelFixture<>(
                new PushChannel(provider),
                PushMessage.builder()
                        .deviceToken("abc123device")
                        .title("Test Push")
                        .body("Hello push")
                        .build(),
                PushMessage.builder()
                        // no deviceToken, no topic -> invalid
                        .body("Missing target")
                        .build(),
                ChannelType.PUSH,
                "MockPush"
        );
    }

    // =========================================================================
    // Contract: valid message -> successful send
    // =========================================================================

    @Nested
    @DisplayName("Contract: valid messages should send successfully")
    class ValidMessageContract {

        @Test
        @DisplayName("Email channel sends valid message")
        void emailChannelSendsValid() {
            var fixture = emailFixture();
            NotificationResult result = fixture.channel.send(fixture.validMessage);
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("SMS channel sends valid message")
        void smsChannelSendsValid() {
            var fixture = smsFixture();
            NotificationResult result = fixture.channel.send(fixture.validMessage);
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Push channel sends valid message")
        void pushChannelSendsValid() {
            var fixture = pushFixture();
            NotificationResult result = fixture.channel.send(fixture.validMessage);
            assertThat(result.isSuccess()).isTrue();
        }
    }

    // =========================================================================
    // Contract: invalid message -> ValidationException before provider call
    // =========================================================================

    @Nested
    @DisplayName("Contract: invalid messages should throw ValidationException")
    class InvalidMessageContract {

        @Test
        @DisplayName("Email channel rejects invalid message")
        void emailChannelRejectsInvalid() {
            var fixture = emailFixture();
            assertThatThrownBy(() -> fixture.channel.send(fixture.invalidMessage))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("SMS channel rejects invalid message")
        void smsChannelRejectsInvalid() {
            var fixture = smsFixture();
            assertThatThrownBy(() -> fixture.channel.send(fixture.invalidMessage))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("Push channel rejects invalid message")
        void pushChannelRejectsInvalid() {
            var fixture = pushFixture();
            assertThatThrownBy(() -> fixture.channel.send(fixture.invalidMessage))
                    .isInstanceOf(ValidationException.class);
        }
    }

    // =========================================================================
    // Contract: channel type and provider name are correct
    // =========================================================================

    @Nested
    @DisplayName("Contract: channel metadata is correct")
    class ChannelMetadataContract {

        @Test
        @DisplayName("Email channel reports correct type and provider")
        void emailMetadata() {
            var fixture = emailFixture();
            assertThat(fixture.channel.getChannelType()).isEqualTo(fixture.expectedType);
            assertThat(fixture.channel.getProviderName()).isEqualTo(fixture.expectedProvider);
        }

        @Test
        @DisplayName("SMS channel reports correct type and provider")
        void smsMetadata() {
            var fixture = smsFixture();
            assertThat(fixture.channel.getChannelType()).isEqualTo(fixture.expectedType);
            assertThat(fixture.channel.getProviderName()).isEqualTo(fixture.expectedProvider);
        }

        @Test
        @DisplayName("Push channel reports correct type and provider")
        void pushMetadata() {
            var fixture = pushFixture();
            assertThat(fixture.channel.getChannelType()).isEqualTo(fixture.expectedType);
            assertThat(fixture.channel.getProviderName()).isEqualTo(fixture.expectedProvider);
        }
    }

    // =========================================================================
    // Contract: provider SendException propagates through channel
    // =========================================================================

    @Nested
    @DisplayName("Contract: SendException propagates from provider")
    class SendExceptionContract {

        @Test
        @DisplayName("Email channel propagates SendException")
        void emailPropagatesSendException() {
            EmailProvider provider = mock(EmailProvider.class);
            when(provider.getName()).thenReturn("FailProvider");
            when(provider.send(any())).thenThrow(new SendException("API down", "FailProvider"));
            EmailChannel channel = new EmailChannel(provider);
            var message = emailFixture().validMessage;

            assertThatThrownBy(() -> channel.send(message))
                    .isInstanceOf(SendException.class)
                    .hasMessageContaining("API down");
        }

        @Test
        @DisplayName("SMS channel propagates SendException")
        void smsPropagatesSendException() {
            SmsProvider provider = mock(SmsProvider.class);
            when(provider.getName()).thenReturn("FailProvider");
            when(provider.send(any())).thenThrow(new SendException("Network error", "FailProvider"));
            SmsChannel channel = new SmsChannel(provider);
            var message = smsFixture().validMessage;

            assertThatThrownBy(() -> channel.send(message))
                    .isInstanceOf(SendException.class)
                    .hasMessageContaining("Network error");
        }

        @Test
        @DisplayName("Push channel propagates SendException")
        void pushPropagatesSendException() {
            PushProvider provider = mock(PushProvider.class);
            when(provider.getName()).thenReturn("FailProvider");
            when(provider.send(any())).thenThrow(new SendException("Token expired", "FailProvider"));
            PushChannel channel = new PushChannel(provider);
            var message = pushFixture().validMessage;

            assertThatThrownBy(() -> channel.send(message))
                    .isInstanceOf(SendException.class)
                    .hasMessageContaining("Token expired");
        }
    }
}
