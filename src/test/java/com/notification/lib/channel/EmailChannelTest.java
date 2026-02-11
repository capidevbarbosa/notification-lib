package com.notification.lib.channel;

import com.notification.lib.channel.email.EmailChannel;
import com.notification.lib.channel.email.EmailMessage;
import com.notification.lib.channel.email.provider.EmailProvider;
import com.notification.lib.core.ChannelType;
import com.notification.lib.core.NotificationResult;
import com.notification.lib.exception.SendException;
import com.notification.lib.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmailChannel")
class EmailChannelTest {

    @Mock
    private EmailProvider emailProvider;

    private EmailChannel emailChannel;

    @BeforeEach
    void setUp() {
        when(emailProvider.getName()).thenReturn("MockProvider");
        emailChannel = new EmailChannel(emailProvider);
    }

    private EmailMessage validEmail() {
        return EmailMessage.builder()
                .recipient("user@example.com")
                .from("noreply@company.com")
                .subject("Test Subject")
                .body("Test body")
                .build();
    }

    @Test
    @DisplayName("should send valid email successfully")
    void shouldSendValidEmail() {
        EmailMessage message = validEmail();
        NotificationResult expectedResult = NotificationResult.success("msg-123", "MockProvider", ChannelType.EMAIL);
        when(emailProvider.send(any(EmailMessage.class))).thenReturn(expectedResult);

        NotificationResult result = emailChannel.send(message);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessageId()).isEqualTo("msg-123");
        verify(emailProvider).send(message);
    }

    @Test
    @DisplayName("should throw ValidationException for invalid email")
    void shouldThrowValidationExceptionForInvalidEmail() {
        EmailMessage invalidMessage = EmailMessage.builder()
                .recipient("not-an-email")
                .build();

        assertThatThrownBy(() -> emailChannel.send(invalidMessage))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Email validation failed");

        verify(emailProvider, never()).send(any());
    }

    @Test
    @DisplayName("should propagate SendException from provider")
    void shouldPropagateSendException() {
        EmailMessage message = validEmail();
        when(emailProvider.send(any())).thenThrow(new SendException("API error", "MockProvider"));

        assertThatThrownBy(() -> emailChannel.send(message))
                .isInstanceOf(SendException.class)
                .hasMessageContaining("API error");
    }

    @Test
    @DisplayName("should return EMAIL channel type")
    void shouldReturnEmailChannelType() {
        assertThat(emailChannel.getChannelType()).isEqualTo(ChannelType.EMAIL);
    }

    @Test
    @DisplayName("should return provider name")
    void shouldReturnProviderName() {
        assertThat(emailChannel.getProviderName()).isEqualTo("MockProvider");
    }

    @Test
    @DisplayName("should reject null provider")
    void shouldRejectNullProvider() {
        assertThatThrownBy(() -> new EmailChannel(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
