package com.notification.lib.config;

import com.notification.lib.channel.email.EmailMessage;
import com.notification.lib.core.ChannelType;
import com.notification.lib.core.NotificationChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationConfig")
class NotificationConfigTest {

    @Mock
    private NotificationChannel<EmailMessage> emailChannel;

    @Test
    @DisplayName("should build config with registered channels")
    void shouldBuildWithChannels() {
        NotificationConfig config = NotificationConfig.builder()
                .registerChannel(ChannelType.EMAIL, emailChannel)
                .build();

        Optional<NotificationChannel<EmailMessage>> channel = config.getChannel(ChannelType.EMAIL);
        assertThat(channel).isPresent();
        assertThat(config.getRegisteredChannels()).containsExactly(ChannelType.EMAIL);
    }

    @Test
    @DisplayName("should return empty for unregistered channel")
    void shouldReturnEmptyForUnregistered() {
        NotificationConfig config = NotificationConfig.builder()
                .registerChannel(ChannelType.EMAIL, emailChannel)
                .build();

        Optional<NotificationChannel<EmailMessage>> channel = config.getChannel(ChannelType.SMS);
        assertThat(channel).isEmpty();
    }

    @Test
    @DisplayName("should fail when no channels registered")
    void shouldFailWhenNoChannels() {
        assertThatThrownBy(() -> NotificationConfig.builder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("At least one notification channel");
    }

    @Test
    @DisplayName("should fail when registering null channel type")
    void shouldFailWithNullChannelType() {
        assertThatThrownBy(() -> NotificationConfig.builder()
                .registerChannel(null, emailChannel))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should fail when registering null channel")
    void shouldFailWithNullChannel() {
        assertThatThrownBy(() -> NotificationConfig.builder()
                .registerChannel(ChannelType.EMAIL, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should build with ProviderConfig")
    void shouldBuildProviderConfig() {
        ProviderConfig config = ProviderConfig.builder()
                .apiKey("test-key")
                .apiSecret("test-secret")
                .domain("example.com")
                .build();

        assertThat(config.getApiKey()).isEqualTo("test-key");
        assertThat(config.getApiSecret()).isEqualTo("test-secret");
        assertThat(config.getDomain()).isEqualTo("example.com");
    }

    @Test
    @DisplayName("should create ProviderConfig with convenience method")
    void shouldCreateWithConvenience() {
        ProviderConfig config = ProviderConfig.withApiKey("my-key");
        assertThat(config.getApiKey()).isEqualTo("my-key");
    }
}
