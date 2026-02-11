package com.notification.lib.validation;

import com.notification.lib.channel.push.PushMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PushMessageValidator")
class PushMessageValidatorTest {

    private PushMessageValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PushMessageValidator();
    }

    @Test
    @DisplayName("should pass with valid push message using deviceToken")
    void shouldPassWithDeviceToken() {
        PushMessage message = PushMessage.builder()
                .deviceToken("fcm-token-abc123")
                .title("New message")
                .body("You have a new message")
                .build();

        List<String> violations = validator.validate(message);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("should pass with valid push message using topic")
    void shouldPassWithTopic() {
        PushMessage message = PushMessage.builder()
                .topic("news")
                .title("Breaking News")
                .body("Something happened!")
                .build();

        List<String> violations = validator.validate(message);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("should fail when message is null")
    void shouldFailWhenNull() {
        List<String> violations = validator.validate(null);
        assertThat(violations).containsExactly("Push message must not be null");
    }

    @Test
    @DisplayName("should fail when neither deviceToken nor topic provided")
    void shouldFailWithNoTarget() {
        PushMessage message = PushMessage.builder()
                .title("Test")
                .body("Body")
                .build();

        List<String> violations = validator.validate(message);
        assertThat(violations).anyMatch(v -> v.contains("deviceToken or topic"));
    }

    @Test
    @DisplayName("should fail when title is missing")
    void shouldFailWhenTitleMissing() {
        PushMessage message = PushMessage.builder()
                .deviceToken("token123")
                .body("Body")
                .build();

        List<String> violations = validator.validate(message);
        assertThat(violations).anyMatch(v -> v.contains("title"));
    }

    @Test
    @DisplayName("should fail with invalid priority")
    void shouldFailWithInvalidPriority() {
        PushMessage message = PushMessage.builder()
                .deviceToken("token123")
                .title("Test")
                .body("Body")
                .priority("urgent")
                .build();

        List<String> violations = validator.validate(message);
        assertThat(violations).anyMatch(v -> v.contains("Priority must be"));
    }

    @Test
    @DisplayName("should pass with valid high priority")
    void shouldPassWithHighPriority() {
        PushMessage message = PushMessage.builder()
                .deviceToken("token123")
                .title("Test")
                .body("Body")
                .priority("high")
                .build();

        List<String> violations = validator.validate(message);
        assertThat(violations).isEmpty();
    }
}
