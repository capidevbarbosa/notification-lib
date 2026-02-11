package com.notification.lib.channel;

import com.notification.lib.channel.push.PushChannel;
import com.notification.lib.channel.push.PushMessage;
import com.notification.lib.channel.push.provider.PushProvider;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PushChannel")
class PushChannelTest {

    @Mock
    private PushProvider pushProvider;

    private PushChannel pushChannel;

    @BeforeEach
    void setUp() {
        when(pushProvider.getName()).thenReturn("MockPushProvider");
        pushChannel = new PushChannel(pushProvider);
    }

    private PushMessage validPush() {
        return PushMessage.builder()
                .deviceToken("fcm-token-abc123")
                .title("Test Notification")
                .body("Test body")
                .data(Map.of("key", "value"))
                .priority("high")
                .build();
    }

    @Test
    @DisplayName("should send valid push notification successfully")
    void shouldSendValidPush() {
        PushMessage message = validPush();
        NotificationResult expected = NotificationResult.success("push-123", "MockPushProvider", ChannelType.PUSH);
        when(pushProvider.send(any(PushMessage.class))).thenReturn(expected);

        NotificationResult result = pushChannel.send(message);

        assertThat(result.isSuccess()).isTrue();
        verify(pushProvider).send(message);
    }

    @Test
    @DisplayName("should throw ValidationException when no target provided")
    void shouldThrowValidationExceptionForNoTarget() {
        PushMessage invalid = PushMessage.builder()
                .title("Test")
                .body("Body")
                .build();

        assertThatThrownBy(() -> pushChannel.send(invalid))
                .isInstanceOf(ValidationException.class);

        verify(pushProvider, never()).send(any());
    }

    @Test
    @DisplayName("should return PUSH channel type")
    void shouldReturnPushChannelType() {
        assertThat(pushChannel.getChannelType()).isEqualTo(ChannelType.PUSH);
    }
}
