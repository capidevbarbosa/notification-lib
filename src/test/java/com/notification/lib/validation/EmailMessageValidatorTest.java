package com.notification.lib.validation;

import com.notification.lib.channel.email.EmailMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmailMessageValidator")
class EmailMessageValidatorTest {

    private EmailMessageValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EmailMessageValidator();
    }

    private EmailMessage.EmailMessageBuilder<?, ?> validEmailBuilder() {
        return EmailMessage.builder()
                .recipient("user@example.com")
                .from("noreply@company.com")
                .subject("Test Subject")
                .body("Test body content");
    }

    @Nested
    @DisplayName("Valid messages")
    class ValidMessages {

        @Test
        @DisplayName("should pass with all required fields")
        void shouldPassWithAllRequiredFields() {
            EmailMessage message = validEmailBuilder().build();
            List<String> violations = validator.validate(message);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("should pass with htmlBody instead of body")
        void shouldPassWithHtmlBody() {
            EmailMessage message = EmailMessage.builder()
                    .recipient("user@example.com")
                    .from("noreply@company.com")
                    .subject("Test")
                    .htmlBody("<h1>Hello</h1>")
                    .build();

            List<String> violations = validator.validate(message);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("should pass with valid CC and BCC")
        void shouldPassWithCcAndBcc() {
            EmailMessage message = validEmailBuilder()
                    .cc(List.of("cc1@example.com", "cc2@example.com"))
                    .bcc(List.of("bcc@example.com"))
                    .build();

            List<String> violations = validator.validate(message);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Invalid messages")
    class InvalidMessages {

        @Test
        @DisplayName("should fail when message is null")
        void shouldFailWhenNull() {
            List<String> violations = validator.validate(null);
            assertThat(violations).containsExactly("Email message must not be null");
        }

        @Test
        @DisplayName("should fail when recipient is missing")
        void shouldFailWhenRecipientMissing() {
            EmailMessage message = EmailMessage.builder()
                    .from("noreply@company.com")
                    .subject("Test")
                    .body("Body")
                    .build();

            List<String> violations = validator.validate(message);
            assertThat(violations).anyMatch(v -> v.contains("Recipient"));
        }

        @Test
        @DisplayName("should fail when recipient email is invalid")
        void shouldFailWhenRecipientInvalid() {
            EmailMessage message = validEmailBuilder()
                    .recipient("not-an-email")
                    .build();

            List<String> violations = validator.validate(message);
            assertThat(violations).anyMatch(v -> v.contains("Recipient email format is invalid"));
        }

        @Test
        @DisplayName("should fail when from is missing")
        void shouldFailWhenFromMissing() {
            EmailMessage message = EmailMessage.builder()
                    .recipient("user@example.com")
                    .subject("Test")
                    .body("Body")
                    .build();

            List<String> violations = validator.validate(message);
            assertThat(violations).anyMatch(v -> v.contains("Sender"));
        }

        @Test
        @DisplayName("should fail when subject is missing")
        void shouldFailWhenSubjectMissing() {
            EmailMessage message = EmailMessage.builder()
                    .recipient("user@example.com")
                    .from("noreply@company.com")
                    .body("Body")
                    .build();

            List<String> violations = validator.validate(message);
            assertThat(violations).anyMatch(v -> v.contains("Subject"));
        }

        @Test
        @DisplayName("should fail when both body and htmlBody are missing")
        void shouldFailWhenBodyMissing() {
            EmailMessage message = EmailMessage.builder()
                    .recipient("user@example.com")
                    .from("noreply@company.com")
                    .subject("Test")
                    .build();

            List<String> violations = validator.validate(message);
            assertThat(violations).anyMatch(v -> v.contains("body"));
        }

        @Test
        @DisplayName("should fail with invalid CC email")
        void shouldFailWithInvalidCc() {
            EmailMessage message = validEmailBuilder()
                    .cc(List.of("invalid-email"))
                    .build();

            List<String> violations = validator.validate(message);
            assertThat(violations).anyMatch(v -> v.contains("Invalid CC email"));
        }

        @Test
        @DisplayName("should collect multiple violations")
        void shouldCollectMultipleViolations() {
            EmailMessage message = EmailMessage.builder().build();
            List<String> violations = validator.validate(message);
            assertThat(violations).hasSizeGreaterThanOrEqualTo(3);
        }
    }
}
