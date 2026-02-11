package com.notification.lib.validation;

import com.notification.lib.channel.sms.SmsMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SmsMessageValidator")
class SmsMessageValidatorTest {

    private SmsMessageValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SmsMessageValidator();
    }

    @Test
    @DisplayName("should pass with valid SMS message")
    void shouldPassWithValidMessage() {
        SmsMessage message = SmsMessage.builder()
                .recipient("+1234567890")
                .body("Hello from SMS!")
                .fromNumber("+0987654321")
                .build();

        List<String> violations = validator.validate(message);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("should fail when message is null")
    void shouldFailWhenNull() {
        List<String> violations = validator.validate(null);
        assertThat(violations).containsExactly("SMS message must not be null");
    }

    @Test
    @DisplayName("should fail with invalid phone number format")
    void shouldFailWithInvalidPhone() {
        SmsMessage message = SmsMessage.builder()
                .recipient("12345")
                .body("Test")
                .fromNumber("+1234567890")
                .build();

        List<String> violations = validator.validate(message);
        assertThat(violations).anyMatch(v -> v.contains("E.164 format"));
    }

    @Test
    @DisplayName("should fail when body exceeds max length")
    void shouldFailWhenBodyTooLong() {
        SmsMessage message = SmsMessage.builder()
                .recipient("+1234567890")
                .body("x".repeat(1601))
                .fromNumber("+1234567890")
                .build();

        List<String> violations = validator.validate(message);
        assertThat(violations).anyMatch(v -> v.contains("exceeds maximum length"));
    }

    @Test
    @DisplayName("should fail when fromNumber is missing")
    void shouldFailWhenFromMissing() {
        SmsMessage message = SmsMessage.builder()
                .recipient("+1234567890")
                .body("Test")
                .build();

        List<String> violations = validator.validate(message);
        assertThat(violations).anyMatch(v -> v.contains("fromNumber"));
    }
}
