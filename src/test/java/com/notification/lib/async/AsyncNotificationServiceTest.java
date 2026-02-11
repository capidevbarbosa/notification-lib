package com.notification.lib.async;

import com.notification.lib.channel.email.EmailMessage;
import com.notification.lib.channel.sms.SmsMessage;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AsyncNotificationService")
class AsyncNotificationServiceTest {

    @Mock
    private NotificationChannel<EmailMessage> emailChannel;

    @Mock
    private NotificationChannel<SmsMessage> smsChannel;

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
    @DisplayName("should send async and return CompletableFuture")
    void shouldSendAsync() throws Exception {
        when(emailChannel.send(any())).thenReturn(
                NotificationResult.success("msg-1", "TestProvider", ChannelType.EMAIL));

        CompletableFuture<NotificationResult> future = asyncService.sendAsync(validEmail());
        NotificationResult result = future.get();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessageId()).isEqualTo("msg-1");
    }

    @Test
    @DisplayName("should handle async failure")
    void shouldHandleAsyncFailure() {
        when(emailChannel.send(any())).thenThrow(new SendException("Provider down", "Test"));

        CompletableFuture<NotificationResult> future = asyncService.sendAsync(validEmail());

        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(SendException.class);
    }

    @Test
    @DisplayName("should send batch of notifications")
    void shouldSendBatch() throws Exception {
        when(emailChannel.send(any())).thenReturn(
                NotificationResult.success("msg-1", "TestProvider", ChannelType.EMAIL));

        List<EmailMessage> messages = List.of(validEmail(), validEmail(), validEmail());

        CompletableFuture<List<NotificationResult>> future = asyncService.sendBatch(messages);
        List<NotificationResult> results = future.get();

        assertThat(results).hasSize(3);
        assertThat(results).allMatch(NotificationResult::isSuccess);
    }

    @Test
    @DisplayName("should send batch settled even with failures")
    void shouldSendBatchSettled() throws Exception {
        when(emailChannel.send(any()))
                .thenReturn(NotificationResult.success("msg-1", "TestProvider", ChannelType.EMAIL))
                .thenThrow(new SendException("Fail", "Test"))
                .thenReturn(NotificationResult.success("msg-3", "TestProvider", ChannelType.EMAIL));

        List<EmailMessage> messages = List.of(validEmail(), validEmail(), validEmail());

        CompletableFuture<List<NotificationResult>> future = asyncService.sendBatchSettled(messages);
        List<NotificationResult> results = future.get();

        assertThat(results).hasSize(3);
        long successCount = results.stream().filter(NotificationResult::isSuccess).count();
        assertThat(successCount).isGreaterThanOrEqualTo(1);
    }
}
