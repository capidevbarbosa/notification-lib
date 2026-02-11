package com.notification.lib.channel;

import com.notification.lib.channel.sms.SmsChannel;
import com.notification.lib.channel.sms.SmsMessage;
import com.notification.lib.channel.sms.provider.SmsProvider;
import com.notification.lib.core.ChannelType;
import com.notification.lib.core.NotificationResult;
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
@DisplayName("SmsChannel")
class SmsChannelTest {

    @Mock
    private SmsProvider smsProvider;

    private SmsChannel smsChannel;

    @BeforeEach
    void setUp() {
        when(smsProvider.getName()).thenReturn("MockSmsProvider");
        smsChannel = new SmsChannel(smsProvider);
    }

    private SmsMessage validSms() {
        return SmsMessage.builder()
                .recipient("+1234567890")
                .body("Test SMS")
                .fromNumber("+0987654321")
                .build();
    }

    @Test
    @DisplayName("should send valid SMS successfully")
    void shouldSendValidSms() {
        SmsMessage message = validSms();
        NotificationResult expected = NotificationResult.success("sms-123", "MockSmsProvider", ChannelType.SMS);
        when(smsProvider.send(any(SmsMessage.class))).thenReturn(expected);

        NotificationResult result = smsChannel.send(message);

        assertThat(result.isSuccess()).isTrue();
        verify(smsProvider).send(message);
    }

    @Test
    @DisplayName("should throw ValidationException for invalid phone")
    void shouldThrowValidationExceptionForInvalidPhone() {
        SmsMessage invalid = SmsMessage.builder()
                .recipient("12345")
                .body("Test")
                .fromNumber("+1234567890")
                .build();

        assertThatThrownBy(() -> smsChannel.send(invalid))
                .isInstanceOf(ValidationException.class);

        verify(smsProvider, never()).send(any());
    }

    @Test
    @DisplayName("should return SMS channel type")
    void shouldReturnSmsChannelType() {
        assertThat(smsChannel.getChannelType()).isEqualTo(ChannelType.SMS);
    }
}
